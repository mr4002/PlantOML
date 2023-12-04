import React, { useState } from 'react';
import FileTree from './/FileTree';
import MonacoEditor from 'react-monaco-editor';
import OptionsComponent from './Options';
import { Resizable } from 're-resizable';
import JSZip from 'jszip';

import ResizeHandle from './/ResizeHandle';
import './Toolbar.css';

import './Server.css';


function Server() {
    const [files, setFiles] = useState([]);
    const [currentFile, setCurrentFile] = useState('');
    const [code, setCode] = useState('');
    const [isImageSelected, setIsImageSelected] = useState(false);
    const [imageDataUrl, setImageDataUrl] = useState('');

    const handleFolderUpload = (event) => {
        const files = event.target.files;
        const fileTree = buildFileTree(Array.from(files));
        setFiles(fileTree);
    };
    
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
        console.log('TEST')
        setCode(newValue);
    };

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

    const processZip = async (blob) => {
        const zip = await JSZip.loadAsync(blob);
        const fileTreeCopy = JSON.parse(JSON.stringify(files)); 
    
        for (const [relativePath, zipEntry] of Object.entries(zip.files)) {
            if (zipEntry.dir) continue; 
    
            const dirPath = getDirPath(relativePath); 
            const dirNode = findNode(fileTreeCopy, dirPath); 
    
            if (dirNode) {
                const fileBlob = await zipEntry.async("blob");
                dirNode.children.push({
                    name: zipEntry.name,
                    type: 'file',
                    path: relativePath,
                    rawFile: fileBlob 
                });
            }
        }
    
        setFiles(fileTreeCopy);
    };

    const getDirPath = (filePath) => {
        const pathParts = filePath.split('/');
        pathParts.pop(); 
        return pathParts.join('/');
    };

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
                        <FileTree files={files} onFileSelect={handleFileSelect} />
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
            </div>
        </div>
    );
}

export default Server;