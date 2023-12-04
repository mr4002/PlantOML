import subprocess
import time
import requests
import zipfile
import filecmp
import os
import shutil


#can keep a file or different files of these test cases instead of generating them here each time
test_cases = {'Kepler0': ('./basicfamily.zip',dict()), 'BasicFamily0': ('./kepler16b-example.zip',dict())}


nodeColors = ['gray']
nodeEdges = ['dotted']
graphLayout = ['neato']
graphBackgroundColor = ['blue']
nodeShape = ['Ellipse']

counter = 1
for n in nodeColors:
    for e in nodeEdges:
        for gl in graphLayout:
            for gbc in graphBackgroundColor:
                for ns in nodeShape:
                    entry = dict()
                    entry['nodeColor'] = n
                    entry['nodeEdges'] = e
                    entry['graphLayout'] = gl
                    entry['graphBackgroundColor'] = gbc
                    entry['nodeShape'] = ns
                    test_cases['Kepler'+str(counter)] = ('./kepler16b-example.zip', entry)
                    counter += 1
counter = 1
for n in nodeColors:
    for e in nodeEdges:
        for gl in graphLayout:
            for gbc in graphBackgroundColor:
                for ns in nodeShape:
                    entry = dict()
                    entry['nodeColor'] = n
                    entry['nodeEdges'] = e
                    entry['graphLayout'] = gl
                    entry['graphBackgroundColor'] = gbc
                    entry['nodeShape'] = ns
                    test_cases['BasicFamily'+str(counter)] = ('./basicfamily.zip', entry)
                    counter += 1


def start_server():
    command = ['./mvnw', 'spring-boot:run']
    process = subprocess.Popen(command,  cwd='../' )
    time.sleep(10) 
    return process

def stop_server(process):
    process.terminate()
    process.communicate()

def run_tests():
    server_process = start_server()

    try:
        for test_name, (zip_file_path, params) in test_cases.items():
            files = {'file': open(zip_file_path, 'rb')} 
            url = 'http://localhost:8080/plantoml/oml/upload'
            response = requests.post(url, files=files, params=params)
            if response.status_code == 200:

                with open('diagrams.zip', 'wb') as f:
                    f.write(response.content)

                with zipfile.ZipFile('diagrams.zip', 'r') as zip_ref:
                    zip_ref.extractall('extracted_files')

                comparison = filecmp.dircmp('extracted_files', f"./truth/{test_name}")
                if comparison.diff_files:
                    print("Files differ")
                    assert(False)
                else:
                    print("Files are the same")

                os.remove('diagrams.zip')
                shutil.rmtree('extracted_files')
    finally:
        # Stop the server after the test
        stop_server(server_process)

def generate_truth_test_cases():
    server_process = start_server()
    url = 'http://localhost:8080/plantoml/oml/upload'
    for test_name, (zip_file_path, params) in test_cases.items():
        files = {'file': open(zip_file_path, 'rb')} 
        response = requests.post(url, files=files, params=params)
        test_folder = f"./truth/{test_name}"
        os.makedirs(test_folder, exist_ok=True)
        
        with open('diagrams.zip', 'wb') as f:
                f.write(response.content)
        with zipfile.ZipFile('diagrams.zip', 'r') as zip_ref:
            zip_ref.extractall(test_folder)
    stop_server(server_process)
        


run_tests()
