from flask import Flask, request, send_file
import os 
import io
import subprocess

app = Flask(__name__)

@app.route('/generate_image', methods=['POST'])
def generate_image():
    try:
        # Receive DOT file from the request
        dot_file = request.files['dot_file']

        # Save the DOT file temporarily
        dot_file_path = 'temp.dot'
        dot_file.save(dot_file_path)

        # Run dot command to generate the image (PNG format)
        image_data = subprocess.check_output(['dot', '-Tpng', dot_file_path])

        # Create an in-memory byte stream buffer
        img_buffer = io.BytesIO(image_data)

        # Send the image as a response
        return send_file(img_buffer, mimetype='image/png', as_attachment=True, download_name='output.png')

    except Exception as e:
        return str(e), 500  # Return an error message and status code 500 in case of an error

if __name__ == '__main__':
    app.run(debug=True)