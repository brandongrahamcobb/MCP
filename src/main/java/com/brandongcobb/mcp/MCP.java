/*  MCP.java The primary purpose of this class is to integrate
 *  local and remote AI tools.
 *
 *  Copyright (C) 2025  github.com/brandongrahamcobb
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.brandongcobb.mcp;

import com.brandongcobb.mcp.component.server.*;
import com.brandongcobb.mcp.service.*;
import org.springframework.context.ApplicationContext;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.*;
import java.io.*;
import jakarta.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

@SpringBootApplication
public class MCP {

    private MCP app;
    private static final Logger LOGGER = Logger.getLogger(MCP.class.getName());
    public static final String BLURPLE = "\033[38;5;61m";
    public static final String BRIGHT_BLUE = "\u001B[94m";
    public static final String BRIGHT_CYAN = "\u001B[96m";
    public static final String CYAN = "\u001B[36m";
    public static final String DODGER_BLUE = "\u001B[38;5;33m";
    public static final String FUCHSIA = "\033[38;5;201m";
    public static final String GOLD = "\033[38;5;220m";
    public static final String GREEN = "\u001B[32m";
    public static final String LIME = "\033[38;5;154m";
    public static final String NAVY = "\u001B[38;5;18m";
    public static final String ORANGE = "\033[38;5;208m";
    public static final String PINK = "\033[38;5;205m";
    public static final String PURPLE = "\u001B[35m";
    public static final String RED = "\u001B[31m";
    public static final String RESET = "\u001B[0m";
    public static final String SKY_BLUE = "\u001B[38;5;117m";
    public static final String TEAL = "\u001B[38;5;30m";
    public static final String VIOLET = "\033[38;5;93m";
    public static final String WHITE = "\u001B[37m";
    public static final String YELLOW = "\u001B[33m";
    
    public static void main(String[] args) {
        LOGGER.setLevel(Level.OFF);
        for (Handler h : LOGGER.getParent().getHandlers()) {
            h.setLevel(Level.OFF);
        }
        ApplicationContext ctx = SpringApplication.run(MCP.class, args);
        MCP app = ctx.getBean(MCP.class);
        CustomMCPServer server = ctx.getBean(CustomMCPServer.class);
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                String line = scanner.nextLine();
                if (app.looksLikeJsonRpc(line)) {
                    String response = server.handleRequest(line).join();
                    if (response != null && !response.isBlank()) {
                        System.out.println(response);
                        System.out.flush();
                    }
                }
            }
        }
    }
    
    private boolean looksLikeJsonRpc(String line) {
        return line.trim().startsWith("{") && line.contains("\"method\"");
    }

    public CompletableFuture<MCP> completeGetAppInstance() {
        return CompletableFuture.completedFuture(this);
    }

}
