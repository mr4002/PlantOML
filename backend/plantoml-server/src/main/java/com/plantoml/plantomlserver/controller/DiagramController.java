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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HexFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.zip.Inflater;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;

import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.parse.Parser;


@RestController
@RequestMapping("/plantoml/oml")
public class DiagramController {

    private static final String PLANT_UML_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-_";
    private static final Base64.Decoder BASE64_DECODER = Base64.getDecoder();
    private final RestTemplate restTemplate;

    private Oml2DotApp oml2DotApp;
    private ArrayList<String> options;

    @Autowired
    public DiagramController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.oml2DotApp = new Oml2DotApp();
    }

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
            options.add("-K=" + graphLayout);
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
            byte[] imageBytes = generateDiagramFromOmlText(decodedText); //TODO: replace with img generator

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

    //Below generates svg with png file extension causing format to be off, use generateDiagramUsingProcessBuilder instead
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
            command.addAll(options);
            command.add("-Tpng");
            command.add(dotFilePath.toString());

            ProcessBuilder processBuilder = new ProcessBuilder("dot", "-Tpng", dotFilePath.toString());
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

    private byte[] generateDiagramFromOmlText(String omlText) {
        //TODO: decode OML into AST then go from AST to Dot here

        System.out.println("===================== OML TEXT START ======================");
        System.out.println(omlText);
        System.out.println("===================== OML TEXT END ======================");



        System.out.println("===================== TRANSLATION START ======================");
        String dot = this.oml2DotApp.parse(omlText);
        System.out.println("===================== TRANSLATION END ======================");


        if (dot != null) {
            return generateDiagramUsingProcessBuilder(dot);
        } else {
            return null;
        }
    }

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

    private byte[] decodePlantUML(String text) {
        String base64 = text.chars()
                .mapToObj(c -> PLANT_UML_ALPHABET.indexOf((char) c))
                .map(i -> "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".charAt(i))
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString();
        return BASE64_DECODER.decode(base64);
    }
}
