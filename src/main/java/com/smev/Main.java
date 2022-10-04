package com.smev;

import com.smev.model.input.InputData;
import com.smev.processors.InputProcessor;
import com.smev.processors.VendoringProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            final InputData inputData = new InputProcessor().process(args);
            new VendoringProcessor().process(inputData.getDeps(), inputData.getRootDirectory());
        } catch (Exception e) {
            log.error("Can't execute program", e);
        }
    }
}
