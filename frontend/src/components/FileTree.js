import React from 'react';
import { Tree } from '@geist-ui/react';

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