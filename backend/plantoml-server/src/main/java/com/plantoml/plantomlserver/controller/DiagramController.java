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
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Base64;
import java.util.zip.Inflater;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/plantoml/oml")
public class DiagramController {

    private static final String PLANT_UML_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-_";
    private static final Base64.Decoder BASE64_DECODER = Base64.getDecoder();
    private final RestTemplate restTemplate;

    private Oml2DotApp oml2DotApp;

    @Autowired
    public DiagramController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.oml2DotApp = new Oml2DotApp();
    }

    @GetMapping("/{textEncoding}")
    public ResponseEntity<byte[]> getDiagram(@PathVariable String textEncoding) {

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

    private byte[] sendRequestToGraphviz(String text) {
        String url = "http://localhost:5001/img"; //flask server URL
        HttpHeaders headers = new HttpHeaders();
        headers.set("Some-Header", "header-value"); //set any headers
    
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class);
    
        return response.getBody();
    }

    private byte[] generateDiagramFromOmlText(String omlText) {
        //TODO: decode OML into AST then go from AST to Dot here

        System.out.println("===================== OML TEXT START ======================");
        System.out.println(omlText);
        System.out.println("===================== OML TEXT END ======================");



        System.out.println("===================== TRANSLATION START ======================");
        String dot = this.oml2DotApp.parse(omlText);
        System.out.println(dot);
        System.out.println("===================== TRANSLATION END ======================");


        if (dot != null) {
            return sendRequestToGraphviz(dot);
        } else  {
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
