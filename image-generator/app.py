from flask import Flask, request, send_file
import os 
import io
import subprocess

app = Flask(__name__)

@app.route('/generate_image', methods=['POST'])
def generate_image():
        # Receive DOT file from the request
        print(request.files.keys())
        dot_file = request.files['dot_file']

        # Save the DOT file temporarily
        dot_file_path = 'temp.dot'
        dot_file.save(dot_file_path)

        # can generate formats other htan png as well
        node_shape = request.form.get('node_shape')
        node_color = request.form.get('node_color')
        edge_color = request.form.get('edge_color')
        edge_style = request.form.get('edge_style')
        graph_layout = request.form.get('graph_layout')
        graph_bg_color = request.form.get('graph_bg_color')
        dpi = request.form.get('dpi')
        dot_command = ['dot', '-Tpng', dot_file_path]


        if node_shape:
            dot_command.insert(1, f'-Nshape={node_shape}')
        if node_color:
            dot_command.insert(1, f'-Ncolor={node_color}')
        if edge_color:
            dot_command.insert(1, f'-Ecolor={edge_color}')
        if edge_style:
            dot_command.insert(1, f'-Estyle={edge_style}')
        if graph_layout:
            dot_command.insert(1, f'-K={graph_layout}')
        if graph_bg_color:
            dot_command.insert(1, f'-Gbgcolor={graph_bg_color}')
        if dpi:
            dot_command.insert(1, f'-Gdpi={dpi}')
        print(dot_command)
        # Run dot command to generate the image (PNG format)
        image_data = subprocess.check_output(dot_command)

        # Create an in-memory byte stream buffer
        img_buffer = io.BytesIO(image_data)

        # Send the image as a response
        return send_file(img_buffer, mimetype='image/png', as_attachment=True, download_name='image.png')


if __name__ == '__main__':
    app.run(debug=True)