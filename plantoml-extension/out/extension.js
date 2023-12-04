"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || function (mod) {
    if (mod && mod.__esModule) return mod;
    var result = {};
    if (mod != null) for (var k in mod) if (k !== "default" && Object.prototype.hasOwnProperty.call(mod, k)) __createBinding(result, mod, k);
    __setModuleDefault(result, mod);
    return result;
};
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.deactivate = exports.activate = void 0;
// The module 'vscode' contains the VS Code extensibility API
// Import the module and reference it with the alias vscode in your code below
const vscode = __importStar(require("vscode"));
const path = __importStar(require("path"));
const decompress_1 = __importDefault(require("decompress"));
const jszip_1 = __importDefault(require("jszip"));
// This method is called when your extension is activated
// Your extension is activated the very first time the command is executed
const fs = vscode.workspace.fs;
const imageZipFolder = ".oml_diagrams";
const imageZipFile = "images.zip";
function activate(context) {
    context.subscriptions.push(vscode.commands.registerCommand("plantoml-extension.visualizeOML", async () => {
        vscode.window.withProgress({
            location: vscode.ProgressLocation.Notification,
            title: "Generating new OML diagrams...",
            cancellable: false,
        }, (progress, task) => {
            return runMain();
        });
    }));
    context.subscriptions.push(vscode.commands.registerCommand("plantoml-extension.previewOMLDiagram", async () => {
        await displayOmlDiagram(context);
    }));
}
exports.activate = activate;
const runMain = async () => {
    const mainFolder = vscode.workspace.workspaceFolders?.[0];
    if (!mainFolder) {
        vscode.window.showErrorMessage("No workspace folder open");
        return;
    }
    const zipBlob = await getZippedBlob(mainFolder.uri);
    await writeImagesZip(zipBlob, mainFolder, imageZipFile);
    (0, decompress_1.default)(path.join(mainFolder.uri.fsPath, imageZipFile), path.join(mainFolder.uri.fsPath, imageZipFolder));
    return "done";
};
const getZippedBlob = async (mainFolderUri) => {
    const zip = new jszip_1.default();
    const zipDirectory = async (directoryUri, zip, root) => {
        if (!zip)
            return;
        let files = [];
        if (!root) {
            files = await vscode.workspace.fs.readDirectory(directoryUri);
        }
        else {
            files = [
                ["build", vscode.FileType.Directory],
                ["src", vscode.FileType.Directory],
                ["catalog.xml", vscode.FileType.File],
            ];
        }
        for (const file of files) {
            const directoryPath = directoryUri.fsPath;
            const filePath = path.join(directoryPath, file[0]);
            const addPath = path.relative(directoryPath, filePath);
            const fileUri = vscode.Uri.parse(filePath);
            const fileStat = await fs.stat(fileUri);
            if (fileStat.type === vscode.FileType.Directory) {
                const subZip = zip.folder(addPath);
                await zipDirectory(fileUri, subZip, false);
                console.log({ path: fileUri.path });
                console.log({ fsPath: fileUri.fsPath });
            }
            else {
                const fileData = await fs.readFile(fileUri);
                zip.file(addPath, fileData);
            }
        }
    };
    await zipDirectory(mainFolderUri, zip, true);
    const zippedBlob = await zip.generateAsync({ type: "blob" });
    return zippedBlob;
};
const writeImagesZip = async (zipBlob, mainFolder, fileName) => {
    const formData = new FormData();
    formData.append("file", zipBlob, "project.zip");
    try {
        const response = await fetch("http://localhost:8080/plantoml/oml/upload", {
            //TODO:replace this with public ipv4 of ec2 instance
            method: "POST",
            body: formData,
        });
        const imageZipPath = path.join(mainFolder.uri.path, fileName);
        const imageZipBlob = await response.blob();
        const arrayBuffer = await imageZipBlob.arrayBuffer();
        const uint8array = new Uint8Array(arrayBuffer);
        await fs.writeFile(vscode.Uri.parse(imageZipPath), uint8array);
    }
    catch (e) {
        console.log({ e });
    }
};
let currentPanel = undefined;
const displayOmlDiagram = async (context) => {
    const activeEditor = vscode.window.activeTextEditor;
    if (!activeEditor) {
        vscode.window.showErrorMessage("No OML file opened");
        return;
    }
    const document = activeEditor.document;
    const fileName = document.fileName;
    if (fileName?.substring(fileName.length - 4) !== ".oml") {
        vscode.window.showErrorMessage("Not an OML file");
        return;
    }
    //find the image uri of
    let diskUri;
    try {
        //check if images directory exists
        const mainFolder = vscode.workspace.workspaceFolders?.[0];
        if (!mainFolder) {
            throw Error("Invalid OML workspace folder");
        }
        const imagesDirPath = path.join(mainFolder.uri.fsPath, imageZipFolder);
        const images = await fs.readDirectory(vscode.Uri.parse(imagesDirPath));
        for (const image of images) {
            const imageName = image[0].substring(0, image[0].length - 4);
            console.log({ 1: imageName, 2: path.basename(fileName) });
            if (imageName + ".oml" === path.basename(fileName)) {
                diskUri = vscode.Uri.parse(path.join(imagesDirPath, image[0]));
            }
        }
    }
    catch (e) {
        console.error(e);
        vscode.window.showErrorMessage("Image not found. Try generating diagrams first.");
        return;
    }
    if (!diskUri) {
        vscode.window.showErrorMessage("Image not found. Try generating diagrams first.");
        return;
    }
    const columnToShowIn = activeEditor ? activeEditor.viewColumn : undefined;
    if (currentPanel) {
        currentPanel.reveal(columnToShowIn);
    }
    else {
        currentPanel = vscode.window.createWebviewPanel("omldiagram", `OML Diagram Preview`, columnToShowIn || vscode.ViewColumn.Beside, {});
    }
    const imageSrc = currentPanel.webview.asWebviewUri(diskUri);
    currentPanel.webview.html = getWebviewContent(imageSrc);
    // Reset when the current panel is closed
    currentPanel.onDidDispose(() => {
        currentPanel = undefined;
    }, null, context.subscriptions);
};
const getWebviewContent = (imageUri) => {
    return `<!DOCTYPE html>
	<html lang="en">
	<head>
		<meta charset="UTF-8">
		<meta name="viewport" content="width=device-width, initial-scale=1.0">
		<title>OML Diagram</title>
	</head>
	<body>
  <img src="${imageUri}" alt="Image">
	</body>
	</html>`;
};
// This method is called when your extension is deactivated
function deactivate() { }
exports.deactivate = deactivate;
//# sourceMappingURL=extension.js.map