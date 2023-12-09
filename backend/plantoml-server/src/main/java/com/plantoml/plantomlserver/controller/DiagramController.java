package com.plantoml.plantomlserver.controller;

import com.plantoml.plantomlserver.translator.Oml2DotApp;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;

import org.springframework.http.HttpHeaders;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Map;
import java.util.zip.Inflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;

import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.parse.Parser;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller class for handling diagram-related HTTP requests.
 * <p>
 * This class manages the web API endpoints for PlantOML, specifically for
 * uploading OML projects and generating diagrams. It includes methods to handle
 * file uploads and to apply various graphical options to the generated diagrams.
 */
@CrossOrigin(origins = "http://localhost:3000") // Allow only a specific origin
@RestController
@RequestMapping("/plantoml/oml")
public class DiagramController {

    private static final String PLANT_UML_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-_";
    private static final Base64.Decoder BASE64_DECODER = Base64.getDecoder();
    private final RestTemplate restTemplate;

    private Oml2DotApp oml2DotApp;
    private ArrayList<String> options;

    /**
     * Constructs a DiagramController with the specified RestTemplate.
     * <p>
     * @param restTemplate The RestTemplate used for HTTP requests within the controller.
     */
    @Autowired
    public DiagramController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.oml2DotApp = new Oml2DotApp();
    }

    /**
     * Handles the HTTP POST request for uploading an OML project.
     * <p>
     * This method processes the uploaded OML project, generates diagrams, and
     * returns them in a ZIP file. It accepts additional parameters for graphical
     * customizations of the diagrams.
     *
     * @param file The uploaded OML project file.
     * @param nodeShape The shape of the nodes in the diagram.
     * @param nodeColor The color of the nodes.
     * @param edgeColor The color of the edges.
     * @param edgeStyle The style of the edges.
     * @param graphLayout The layout algorithm for the graph.
     * @param graphBgColor The background color of the graph.
     * @param dpi The DPI for the graph images.
     * @return A ResponseEntity containing either the ZIP file of diagrams or an error message.
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadOmlProject(@RequestParam("file") MultipartFile file,        
        @RequestParam(value = "nodeShape", required = false) String nodeShape,
        @RequestParam(value = "nodeColor",required = false) String nodeColor,
        @RequestParam(value = "edgeColor",required = false) String edgeColor,
        @RequestParam(value = "edgeStyle",required = false) String edgeStyle,
        @RequestParam(value = "graphLayout",required = false) String graphLayout,
        @RequestParam(value = "graphBgColor",required = false) String graphBgColor,
        @RequestParam(value = "dpi",required = false) String dpi) {
        this.options = new ArrayList<>();
        if (nodeShape != null && !nodeShape.isEmpty()) {
            this.options.add("-Nshape=" + nodeShape);
        }
        if (nodeColor != null && !nodeColor.isEmpty()) {
            this.options.add("-Ncolor=" + nodeColor);
        }
        if (edgeColor != null && !edgeColor.isEmpty()) {
            this.options.add("-Ecolor=" + edgeColor);
        }
        if (edgeStyle != null && !edgeStyle.isEmpty()) {
            this.options.add("-Estyle=" + edgeStyle);
        }
        if (graphLayout != null && !graphLayout.isEmpty()) {
            this.options.add("-K" + graphLayout);
        }
        if (graphBgColor != null && !graphBgColor.isEmpty()) {
            this.options.add("-Gbgcolor=" + graphBgColor);
        }
        if (dpi != null && !dpi.isEmpty()) {
            this.options.add("-Gdpi=" + dpi);
        }
        System.out.println("HERE");
        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File is empty");
        }

        try {
            Path tempDir = Files.createTempDirectory("oml-project-");
            Path zipFilePath = tempDir.resolve(file.getOriginalFilename());
            file.transferTo(zipFilePath.toFile());

            // Extract the ZIP file
            extractZipFile(zipFilePath, tempDir);

            // Process each OML file and generate diagrams
            Map<String, byte[]> diagrams = new HashMap<>();
            Files.walk(tempDir)
                    .filter(path -> path.toString().endsWith(".oml") && !path.toString().contains("build"))
                    .forEach(omlFilePath -> {
                        byte[] imageBytes = generateDiagramFromOmlFile(omlFilePath);
                        if (imageBytes != null) {
                            diagrams.put(omlFilePath.getFileName().toString().replace(".oml", ".png"), imageBytes);
                        }
                    });

            // Bundle the images into a single ZIP file
            Path outputZip = bundleDiagramsIntoZip(diagrams, tempDir);

            // Return the ZIP file
            byte[] zipBytes = Files.readAllBytes(outputZip);

            // Clean up
            deleteDirectory(tempDir);

            return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(zipBytes);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing file");
        }
    }

    /**
     * Handles the HTTP GET request to generate a diagram based on the provided text encoding.
     * <p>
     * This method decodes the text encoding, applies optional diagram customization options,
     * and returns a PNG image of the generated diagram.
     *
     * @param textEncoding The encoded text representing the OML or other diagram information.
     * @param nodeShape Optional customization for the shape of nodes in the diagram.
     * @param nodeColor Optional customization for the color of nodes.
     * @param edgeColor Optional customization for the color of edges.
     * @param edgeStyle Optional customization for the style of edges.
     * @param graphLayout Optional customization for the graph layout algorithm.
     * @param graphBgColor Optional customization for the background color of the graph.
     * @param dpi Optional customization for the DPI of the graph image.
     * @return A ResponseEntity containing either the PNG image of the diagram or an error message.
     */
    @GetMapping("/{textEncoding}")
    public ResponseEntity<byte[]> getDiagram(@PathVariable String textEncoding,
        @RequestParam(required = false) String nodeShape,
        @RequestParam(required = false) String nodeColor,
        @RequestParam(required = false) String edgeColor,
        @RequestParam(required = false) String edgeStyle,
        @RequestParam(required = false) String graphLayout,
        @RequestParam(required = false) String graphBgColor,
        @RequestParam(required = false) String dpi
    ) {
        this.options = new ArrayList<>();
        if (nodeShape != null && !nodeShape.isEmpty()) {
            this.options.add("-Nshape=" + nodeShape);
        }
        if (nodeColor != null && !nodeColor.isEmpty()) {
            this.options.add("-Ncolor=" + nodeColor);
        }
        if (edgeColor != null && !edgeColor.isEmpty()) {
            this.options.add("-Ecolor=" + edgeColor);
        }
        if (edgeStyle != null && !edgeStyle.isEmpty()) {
            this.options.add("-Estyle=" + edgeStyle);
        }
        if (graphLayout != null && !graphLayout.isEmpty()) {
            this.options.add("-K=" + graphLayout);
        }
        if (graphBgColor != null && !graphBgColor.isEmpty()) {
            this.options.add("-Gbgcolor=" + graphBgColor);
        }
        if (dpi != null && !dpi.isEmpty()) {
            this.options.add("-Gdpi=" + dpi);
        }

        try {
            String decodedText;
            //decode hex to text
            if (textEncoding.startsWith("~h")){
                decodedText = new String(HexFormat.of().parseHex(textEncoding.substring(2)), StandardCharsets.UTF_8);
            }
            else if (textEncoding.startsWith("~p")){ //translate text in plantuml's format w/ base64-like encoding
                byte[] compressedBytes = decodePlantUML(textEncoding.substring(2));
                decodedText = new String(decompressDeflate(compressedBytes));
            }
            else { //translate text in plantuml's format w/ base64
                byte[] compressedBytes = BASE64_DECODER.decode(textEncoding);
                decodedText = new String(decompressDeflate(compressedBytes));
            }

            //gen png from decoded text
            byte[] imageBytes = null;//generateDiagramFromOmlText(decodedText);

            //return img in responseentity
            return ResponseEntity
                    .ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(imageBytes);
        } catch (IllegalArgumentException e) {
            //when encoded text is invalid
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(null);
        } catch (Exception e) {
            //all other exceptions
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    /**
     * Generates a diagram from the given DOT (Graphviz) format string.
     * <p>
     * This method uses the Graphviz Java library to generate a PNG image from the DOT format.
     * Note: This method generates SVG with a PNG file extension which might cause format issues.
     *
     * @param dot The string representation of the graph in DOT format.
     * @return A byte array representing the generated PNG image.
     */
    private byte[] generateDiagramFromDot(String dot) {
        try {
            MutableGraph g = new Parser().read(dot);
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                Graphviz.fromGraph(g).render(Format.PNG).toOutputStream(baos);
                return baos.toByteArray();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Generates a diagram using an external Graphviz process.
     * <p>
     * This method writes the DOT format string to a temporary file and uses the Graphviz 'dot' command
     * to generate a PNG image. This approach is used to avoid format issues with the Graphviz Java library.
     *
     * @param dot The string representation of the graph in DOT format.
     * @return A byte array representing the generated PNG image.
     */
    private byte[] generateDiagramUsingProcessBuilder(String dot) {
        Path dotFilePath = null;
        try {
            //write the DOT string to a temp file
            dotFilePath = Files.createTempFile("graph", ".dot");
            Files.writeString(dotFilePath, dot);

            //exe Graphviz process
            //build command
            ArrayList<String> command = new ArrayList<String>();
            command.add("dot");
            System.out.print(options);
            if (this.options != null) {
                command.addAll(options);
            }
            command.add("-Tpng");
            command.add(dotFilePath.toString());
            System.out.print(command);
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();

            //process png
            try (InputStream inputStream = process.getInputStream();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                process.waitFor();
                return outputStream.toByteArray();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (dotFilePath != null) {
                try {
                    Files.delete(dotFilePath);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }


    /**
     * Decompresses a byte array using the Deflate algorithm.
     * <p>
     * This method inflates the compressed data provided in the byte array and returns the decompressed data.
     *
     * @param compressedBytes The compressed data in byte array format.
     * @return A byte array containing the decompressed data.
     * @throws Exception if decompression fails.
     */
    private byte[] decompressDeflate(byte[] compressedBytes) throws Exception {
        Inflater inflater = new Inflater();
        inflater.setInput(compressedBytes);

        byte[] buffer = new byte[1024];
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(compressedBytes.length);

        while (!inflater.finished()) {
            int count = inflater.inflate(buffer);
            outputStream.write(buffer, 0, count);
        }
        outputStream.close();
        byte[] output = outputStream.toByteArray();

        inflater.end();
        return output;
    }


    /**
     * Decodes a PlantUML encoded string into a byte array.
     * <p>
     * This method converts the custom PlantUML encoding to standard Base64 encoding and then decodes it.
     *
     * @param text The PlantUML encoded string.
     * @return A byte array obtained by decoding the PlantUML encoded string.
     */
    private byte[] decodePlantUML(String text) {
        String base64 = text.chars()
                .mapToObj(c -> PLANT_UML_ALPHABET.indexOf((char) c))
                .map(i -> "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".charAt(i))
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString();
        return BASE64_DECODER.decode(base64);
    }


    /**
     * Extracts the contents of a ZIP file into a specified directory.
     * <p>
     * This method iterates through each entry in the ZIP file and extracts files and directories accordingly.
     *
     * @param zipFilePath The path to the ZIP file.
     * @param destDir The destination directory to extract the contents.
     * @throws IOException if an I/O error occurs.
     */
    public void extractZipFile(Path zipFilePath, Path destDir) throws IOException {
        try (ZipInputStream zipIn = new ZipInputStream(Files.newInputStream(zipFilePath))) {
            ZipEntry entry;
            while ((entry = zipIn.getNextEntry()) != null) {
                Path filePath = destDir.resolve(entry.getName());
                if (!entry.isDirectory()) {
                    extractFile(zipIn, filePath);
                } else {
                    Files.createDirectories(filePath);
                }
                zipIn.closeEntry();
            }
        }
    }

    /**
     * Extracts a single file from a ZipInputStream and writes it to the specified file path.
     * <p>
     * @param zipIn The ZipInputStream.
     * @param filePath The file path where the extracted file should be saved.
     * @throws IOException if an I/O error occurs.
     */
    private void extractFile(ZipInputStream zipIn, Path filePath) throws IOException {
        Files.createDirectories(filePath.getParent());
        Files.copy(zipIn, filePath);
    }

    /**
     * Deletes a directory and its contents recursively.
     * <p>
     * This method deletes all files and subdirectories within the specified directory.
     *
     * @param path The path to the directory to be deleted.
     * @throws IOException if an I/O error occurs during file deletion.
     */
    private void deleteDirectory(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> entries = Files.newDirectoryStream(path)) {
                for (Path entry : entries) {
                    deleteDirectory(entry);
                }
            }
        }
        Files.delete(path);
    }

    /**
     * Bundles a collection of diagrams into a single ZIP file.
     * <p>
     * This method creates a ZIP file and adds each diagram from the provided map as a separate entry.
     *
     * @param diagrams A map of diagram file names to their byte array representations.
     * @param tempDir The directory where the ZIP file will be created.
     * @return The path to the created ZIP file.
     * @throws IOException if an I/O error occurs during ZIP file creation.
     */
    private Path bundleDiagramsIntoZip(Map<String, byte[]> diagrams, Path tempDir) throws IOException {
        Path zipPath = tempDir.resolve("diagrams.zip");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            for (Map.Entry<String, byte[]> entry : diagrams.entrySet()) {
                ZipEntry zipEntry = new ZipEntry(entry.getKey());
                zos.putNextEntry(zipEntry);
                zos.write(entry.getValue());
                zos.closeEntry();
            }
        }
        return zipPath;
    }

    /**
     * Generates a diagram from the content of an OML file.
     * <p>
     * This method reads the OML file, converts its content to DOT format, and then uses an external process
     * to generate a diagram.
     *
     * @param omlFilePath The path to the OML file.
     * @return A byte array representing the generated diagram, or null if the diagram could not be generated.
     */
    private byte[] generateDiagramFromOmlFile(Path omlFilePath) {
        try {
            String dot = oml2DotApp.parse(omlFilePath);
            if (dot != null) {
                return generateDiagramUsingProcessBuilder(dot);
            } else {
                return null;
            }
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
    }
}
