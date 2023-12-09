import React from 'react';
import { Tree } from '@geist-ui/react';


/**
 * FileTree component to display a file structure in a tree format.
 * 
 * This component takes a list of files and folders and renders them
 * as a navigable tree structure using the Geist UI Tree component.
 *
 * @param {Object[]} files - Array of file and folder objects to be displayed in the tree.
 * @param {Function} onFileSelect - Callback function to handle the selection of a file.
 */
function FileTree({ files, onFileSelect }) {
    const renderTree = (nodes) => {
        return nodes.map(node => {
            if (node.type === 'folder') {
                return (
                    <Tree.Folder name={node.name} key={node.name}>
                        {renderTree(node.children)}
                    </Tree.Folder>
                );
            } else {
                return (
                    <Tree.File name={node.name} key={node.name} onClick={() => onFileSelect(node.path)} />
                );
            }
        });
    };

    return (
        <Tree>
            {renderTree(files)}
        </Tree>
    );
}

export default FileTree;