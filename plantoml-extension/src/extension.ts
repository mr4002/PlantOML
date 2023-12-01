// The module 'vscode' contains the VS Code extensibility API
// Import the module and reference it with the alias vscode in your code below
import * as vscode from "vscode";
import axios from "axios";
import * as path from "path";

// This method is called when your extension is activated
// Your extension is activated the very first time the command is executed
export function activate(context: vscode.ExtensionContext) {
  context.subscriptions.push(
    vscode.commands.registerCommand(
      "plantoml-extension.visualizeOML",
      async () => {
        const editor = vscode.window.activeTextEditor;
        if (editor) {
          if (
            editor.document.fileName.substring(
              editor.document.fileName.length - 4
            ) !== ".oml"
          ) {
            vscode.window.showErrorMessage("Only visualize OML files");
          }
          const omlCode = editor.document.getText();
          await getImage(context, omlCode);
        } else {
          vscode.window.showErrorMessage("No active text editor found");
        }
      }
    )
  );
}

const getImage = async (context: vscode.ExtensionContext, omlCode: string) => {
  console.log({ omlCode });
  try {
    const response = await axios.get(
      "https://picsum.photos/seed/picsum/200/300"
    );
    console.log(response);
    const imageBuffer = Buffer.from(response.data, "binary");
    console.log({ imageBuffer });
    await displayImageFromBuffer(context, imageBuffer);
  } catch (e: unknown) {
    console.log({ e });
    vscode.window.showErrorMessage(`Error: ${e}`);
  }
};

let currentPanel: vscode.WebviewPanel | undefined = undefined;

const displayImageFromBuffer = async (
  context: vscode.ExtensionContext,
  imageBuffer: Buffer
) => {
  const columnToShowIn = vscode.window.activeTextEditor
    ? vscode.window.activeTextEditor.viewColumn
    : undefined;
  if (!currentPanel) {
    currentPanel = vscode.window.createWebviewPanel(
      "omldiagram",
      "OML Diagram",
      columnToShowIn || vscode.ViewColumn.Beside,
      {}
    );
  }
  const imageUri = await getImageUri(context, imageBuffer);
  console.log({ imageUri });
  currentPanel.webview.html = getWebviewContent(imageUri);

  // Reset when the current panel is closed
  currentPanel.onDidDispose(
    () => {
      currentPanel = undefined;
    },
    null,
    context.subscriptions
  );
};

const getImageUri = async (
  context: vscode.ExtensionContext,
  imageBuffer: Buffer
) => {
  const imageBase64 = imageBuffer.toString("base64");
  console.log({ imageBase64 });
  const imageMimeType = "image/jpeg";
  console.log(imageBase64.length);
  return `data:${imageMimeType};base64,${imageBase64}`;
};

const getWebviewContent = (imageUri: string) => {
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
export function deactivate() {}
