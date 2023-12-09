import React, { useState } from 'react';
import FileTree from './FileTree';
import MonacoEditor from 'react-monaco-editor';
import OptionsComponent from './Options';
import { Resizable } from 're-resizable';
import JSZip from 'jszip';

import ResizeHandle from './/ResizeHandle';
import './Toolbar.css';

import './Server.css';

/**
 * Server component handling the main logic of the PlantOML app.
 * 
 * This component manages the state and interactions for file uploading, selecting, 
 * editing, submitting, and downloading in the PlantOML application.
 */
function Server() {
    const [files, setFiles] = useState([]);
    const [currentFile, setCurrentFile] = useState('');
    const [code, setCode] = useState('');
    const [isImageSelected, setIsImageSelected] = useState(false);
    const [imageDataUrl, setImageDataUrl] = useState('');

    /**
     * Handles the event when a folder is uploaded.
     * Builds a file tree from the uploaded folder.
     * 
     * @param {Object} event - The event object containing the uploaded folder.
     */
    const handleFolderUpload = (event) => {
        const files = event.target.files;
        const fileTree = buildFileTree(Array.from(files));
        setFiles(fileTree);
    };
    
    /**
     * Builds a file tree structure from a list of files.
     * 
     * @param {Object[]} fileList - Array of file objects.
     * @returns {Object[]} The constructed file tree.
     */
    const buildFileTree = (fileList) => {
        const tree = [];
    
        const getOrCreateNode = (pathParts, parentNode) => {
            if (pathParts.length === 0) return parentNode;
    
            const part = pathParts.shift();
            let node = parentNode.children.find(n => n.name === part && n.type === 'folder');
    
            if (!node) {
                node = { name: part, type: 'folder', children: [] };
                parentNode.children.push(node);
            }
    
            return getOrCreateNode(pathParts, node);
        };
    
        fileList.forEach(file => {
            const pathParts = file.webkitRelativePath.split('/').slice(0, -1); 
            const parentNode = { children: tree };
            const node = getOrCreateNode(pathParts, parentNode);
    
            node.children.push({
                name: file.name,
                type: 'file',
                path: file.webkitRelativePath || file.name, 
                rawFile: file 
            });
        });
    
        return tree;
    };

    /**
     * Handles the selection of a file from the file tree.
     * Sets the current file and loads its content into the editor or image viewer.
     * 
     * @param {string} filename - The name of the selected file.
     */
    const handleFileSelect = async (filename) => {
        setCurrentFile(filename);
        const fileExtension = filename.split('.').pop().toLowerCase();
    
        // Flatten file tree to search for the file
        const flattenTree = (node, fileList = []) => {
            if (node.type === 'file') {
                fileList.push(node);
            } else if (node.children) {
                node.children.forEach(child => flattenTree(child, fileList));
            }
            return fileList;
        };
    
        const flatFiles = flattenTree({ children: files });
        const fileObj = flatFiles.find(file => file.path && file.path.endsWith(filename));
    
        if (fileObj && fileObj.rawFile) {
            if (fileExtension === 'png' || fileExtension === 'svg') {
                try {
                    const imageUrl = await readFileAsDataURL(fileObj.rawFile);
                    setIsImageSelected(true);
                    setImageDataUrl(imageUrl);
                } catch (error) {
                    console.error("Error reading image file:", error);
                }
            } else {
                setIsImageSelected(false);
                try {
                    const content = await readFileContent(fileObj.rawFile);
                    setCode(content);
                } catch (error) {
                    console.error("Error reading file content:", error);
                    setCode('');
                }
            }
        }
    };

        // Utility functions for file reading

    const readFileAsDataURL = (file) => {
        return new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = (event) => resolve(event.target.result);
            reader.onerror = (error) => reject(error);
            reader.readAsDataURL(file);
        });
    };

    const readFileContent = (file) => {
        return new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = (event) => resolve(event.target.result);
            reader.onerror = (error) => reject(error);
            reader.readAsText(file);
        });
    };
    
    const handleEditorChange = (newValue) => {
        setCode(newValue);
    };


    /**
     * Submits the current state of the project to the backend for processing.
     * The method constructs a zip file containing the project files and sends it to the backend.
     */
    const handleSubmit = async () => {
        //handle null options
        var selectElements = document.querySelectorAll('select');

        const zip = new JSZip();
    
        // Recursively add files to the zip
        const addFilesToZip = (files, path = '') => {
            files.forEach(file => {
                if (file.type === 'file') {
                    zip.file(path + file.name, file.rawFile);
                } else if (file.type === 'folder') {
                    addFilesToZip(file.children, path + file.name + '/');
                }
            });
        };
    
        addFilesToZip(files);
    
        try {
            const zipBlob = await zip.generateAsync({ type: 'blob' });
            const formData = new FormData();
            formData.append('file', zipBlob, 'project.zip');
            for (var i = 0; i < selectElements.length; i++) {
                var currentValue = selectElements[i].value;
                if (currentValue !== '') {
                    console.log(selectElements[i].name)
                    console.log(selectElements[i].value)
                    formData.append(selectElements[i].name, selectElements[i].value)
                }
            }
            console.log(formData.getAll('nodeColor'))
    
            const response = await fetch('http://localhost:8080/plantoml/oml/upload', { //TODO:replace this with public ipv4 of ec2 instance
                method: 'POST',
                body: formData
            });
    
            if (response.ok) {
                const blob = await response.blob();
                processZip(blob);
                console.log("CAUGHT IT!!")
            } else {
                console.error('Server error:', response.status);
            }
        } catch (error) {
            console.error('Error submitting project:', error);
        }
    };

    /**
     * Downloads the current project and diagrams as a zip file.
     * This method zips the project files and diagrams and triggers a download in the browser.
     */

    const handleDownload = async () => {
        const zip = new JSZip();
    
        const addFilesToZip = (files, path = '') => {
            files.forEach(file => {
                if (file.type === 'file') {
                    console.log(file.name, file.rawFile);
                    zip.file(path + file.name, file.rawFile);
                } else if (file.type === 'folder') {
                    addFilesToZip(file.children, path + file.name + '/');
                }
            });
        };
        
        addFilesToZip(files);
    
        try {
            const zipBlob = await zip.generateAsync({ type: 'blob' });
            const url = URL.createObjectURL(zipBlob);
    
            const a = document.createElement("a");
            document.body.appendChild(a);
            a.style = "display: none";
            a.href = url;
            a.download = 'project_and_diagrams.zip';
            a.click();
            window.URL.revokeObjectURL(url);
            a.remove();
        } catch (error) {
            console.error('Error creating download:', error);
        }
    };
    
    /**
 * Deeply copies a file tree structure.
 * 
 * This function creates a deep copy of a file tree, ensuring that each node,
 * especially file nodes with Blob objects, are correctly duplicated.
 * 
 * @param {Object[]} nodes - The file tree nodes to copy.
 * @returns {Object[]} A deep copy of the file tree.
 */
    const deepCopyFileTree = (nodes) => { //thanks chatgpt
        return nodes.map(node => {
            if (node.type === 'folder') {
                return { ...node, children: deepCopyFileTree(node.children) };
            } else {
                // For file nodes, create a new object and copy the rawFile explicitly
                return { ...node, rawFile: node.rawFile };
            }
        });
    };

    /**
 * Processes a zip file containing diagrams and integrates them into the file tree.
 * 
 * This function loads a zip file, extracts its contents, and adds them under a 
 * new 'diagrams' directory in the file tree.
 * 
 * @param {Blob} blob - The blob representing the zip file.
 */
    const processZip = async (blob) => {
        console.log("HERE")
        console.log(files)
        const zip = await JSZip.loadAsync(blob);
        const fileTreeCopy = deepCopyFileTree(files); //THIS IS ESSENTIAL
        console.log(fileTreeCopy)
        const diagramsDir = {
            name: 'diagrams',
            type: 'folder',
            children: [],
            path: '/diagrams/'
        };
    
        fileTreeCopy.push(diagramsDir);
    
        // Process each file in the ZIP
        for (const [relativePath, zipEntry] of Object.entries(zip.files)) {
            if (zipEntry.dir) continue;
    
            const fileBlob = await zipEntry.async("blob");
            console.log(zipEntry.name, fileBlob instanceof Blob);
            diagramsDir.children.push({
                name: zipEntry.name,
                type: 'file',
                path: 'diagrams/' + zipEntry.name,
                rawFile: fileBlob
            });
        }
        setFiles(fileTreeCopy);
    };

    /**
 * Retrieves the directory path from a file path.
 * 
 * @param {string} filePath - The full path of the file.
 * @returns {string} The directory path of the file.
 */
    const getDirPath = (filePath) => {
        const pathParts = filePath.split('/');
        pathParts.pop(); 
        return pathParts.join('/');
    };

    /**
 * Finds a node in the file tree based on its path.
 * 
 * @param {Object[]} nodes - The file tree nodes to search through.
 * @param {string} path - The path to search for.
 * @returns {Object|null} The found node or null if not found.
 */
    const findNode = (nodes, path) => {
        for (const node of nodes) {
            if (node.type === 'folder' && node.path === path) {
                return node;
            } else if (node.type === 'folder') {
                const foundNode = findNode(node.children, path);
                if (foundNode) return foundNode;
            }
        }
        return null;
    };

    return (
        <div className="App">
            <div className="container">
                <Resizable
                    defaultSize={{
                        width: '20vw',
                        height: '100%'
                    }}
                    minWidth="10%"
                    maxWidth="40%"
                    enable={{ right: true }}
                    handleComponent={{
                        right: <ResizeHandle />
                    }}
                >
                    <div className="file-tree">
                        <input type="file" webkitdirectory="true" onChange={handleFolderUpload} />
                        <FileTree className="file-tree" files={files} onFileSelect={handleFileSelect}/>
                    </div>
                </Resizable>
                <div className="editor">
                    {isImageSelected ? (
                        <img 
                        src={imageDataUrl} 
                        alt={currentFile} 
                        style={{ maxWidth: '100%', maxHeight: '100%', height: 'auto' }} 
                    />
                    ) : (
                    <MonacoEditor
                        width="100%"
                        height="100%"
                        language="javascript"
                        theme="vs-light"
                        value={code}
                        onChange={handleEditorChange}
                        />
                    )}
                </div>
            </div>
            <div className="toolbar">
                <OptionsComponent/>
                <button onClick={handleSubmit}>Submit</button>
                <button onClick={handleDownload}>Download</button>
            </div>
        </div>
    );
}

export default Server;