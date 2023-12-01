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
const axios_1 = __importDefault(require("axios"));
// This method is called when your extension is activated
// Your extension is activated the very first time the command is executed
function activate(context) {
    context.subscriptions.push(vscode.commands.registerCommand("plantoml-extension.visualizeOML", async () => {
        const editor = vscode.window.activeTextEditor;
        if (editor) {
            if (editor.document.fileName.substring(editor.document.fileName.length - 4) !== ".oml") {
                vscode.window.showErrorMessage("Only visualize OML files");
            }
            const omlCode = editor.document.getText();
            await getImage(context, omlCode);
        }
        else {
            vscode.window.showErrorMessage("No active text editor found");
        }
    }));
}
exports.activate = activate;
const getImage = async (context, omlCode) => {
    console.log({ omlCode });
    try {
        const response = await axios_1.default.get("https://picsum.photos/seed/picsum/200/300");
        console.log(response);
        const imageBuffer = Buffer.from(response.data, "binary");
        console.log({ imageBuffer });
        await displayImageFromBuffer(context, imageBuffer);
    }
    catch (e) {
        console.log({ e });
        vscode.window.showErrorMessage(`Error: ${e}`);
    }
};
let currentPanel = undefined;
const displayImageFromBuffer = async (context, imageBuffer) => {
    const columnToShowIn = vscode.window.activeTextEditor
        ? vscode.window.activeTextEditor.viewColumn
        : undefined;
    if (!currentPanel) {
        currentPanel = vscode.window.createWebviewPanel("omldiagram", "OML Diagram", columnToShowIn || vscode.ViewColumn.Beside, {});
    }
    const imageUri = await getImageUri(context, imageBuffer);
    console.log({ imageUri });
    currentPanel.webview.html = getWebviewContent(imageUri);
    // Reset when the current panel is closed
    currentPanel.onDidDispose(() => {
        currentPanel = undefined;
    }, null, context.subscriptions);
};
const getImageUri = async (context, imageBuffer) => {
    const imageBase64 = imageBuffer.toString("base64");
    console.log({ imageBase64 });
    const imageMimeType = "image/jpeg";
    console.log(imageBase64.length);
    return `data:${imageMimeType};base64,${imageBase64}`;
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
		<img src="${imageUri}"/>
	</body>
	</html>`;
};
// This method is called when your extension is deactivated
function deactivate() { }
exports.deactivate = deactivate;
//# sourceMappingURL=extension.js.map