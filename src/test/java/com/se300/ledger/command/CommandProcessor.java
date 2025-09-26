package com.se300.ledger.command;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.se300.ledger.*;

/**
 * CommandProcessor class implementation designed to process individual Blockchain commands
 *
 * @author  Sergey L. Sundukovskiy
 * @version 1.0
 * @since   2025-09-25
 */
public class CommandProcessor {

    private static Ledger ledger = null;

    public static void processCommand(String command) throws CommandProcessorException {

        List<String> tokens = new ArrayList<>();
        //Split the line into tokens between spaces and quotes
        Matcher matcher = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(command);
        while (matcher.find())
            tokens.add(matcher.group(1).replace("\"", ""));

        switch (tokens.get(0)) {
            case "create-ledger" -> {
                if(tokens.size() != 6)
                    throw new CommandProcessorException("create-ledger", "Missing Arguments");

                System.out.println("Creating Ledger: " + tokens.get(1) + " " + tokens.get(3) + " " + tokens.get(5));
                ledger = Ledger.getInstance(tokens.get(1), tokens.get(3), tokens.get(5));
            }
            case "create-account" -> {
                if(tokens.size() != 2)
                    throw new CommandProcessorException("create-account", "Missing Arguments");

                System.out.println("Creating Account: " + tokens.get(1));
                try {
                    ledger.createAccount(tokens.get(1));
                } catch (LedgerException e) {
                    System.out.println("Failed due to: " + e.getReason());
                }
            }
            case "get-account-balance" -> {
                if(tokens.size() != 2)
                    throw new CommandProcessorException("create-account", "Missing Arguments");

                System.out.println("Getting Balance for: " + tokens.get(1));
                try {
                    System.out.println("Account Balance for: " + tokens.get(1) + " is "
                            + ledger.getAccountBalance(tokens.get(1)));

                } catch (LedgerException e) {
                    System.out.println("Failed due to: " + e.getReason());
                }
            }
            case "get-account-balances" -> {
                System.out.println("Getting All Balances");

                Map<String,Integer> map = ledger.getAccountBalances();

                if(map == null){
                    System.out.println("No Account Has Been Committed");
                    break;
                }

                Set<String> keys = new HashSet<>(map.keySet());

                for (String key : keys) {
                    System.out.println("Account Balance for: " + key + " is " + map.get(key));
                }
            }
            case "process-transaction" -> {

                if(tokens.size() != 12)
                    throw new CommandProcessorException("process-transaction", "Missing Arguments");

                System.out.println("Processing Transaction: " + tokens.get(1) + " "
                        + tokens.get(3) + " " + tokens.get(5) + " " + tokens.get(7) + " "
                        + tokens.get(9) + " " + tokens.get(11) + " ");

                Block block = ledger.getUncommittedBlock();

                Account payer = block.getAccount (tokens.get(9));
                Account receiver = block.getAccount(tokens.get(11));

                if(payer == null || receiver == null){
                    throw new CommandProcessorException("process-transaction", "Account Does Not Exist") ;
                }

                Transaction tempTransaction = new Transaction(tokens.get(1), Integer.parseInt(tokens.get(3)),
                        Integer.parseInt(tokens.get(5)), tokens.get(7), payer, receiver);
                try {
                    ledger.processTransaction(tempTransaction);
                } catch (LedgerException e) {
                    System.out.println("Failed due to: " + e.getReason());
                }
            }
            case "get-block" -> {

                if(tokens.size() != 2)
                    throw new CommandProcessorException("get-block", "Missing Arguments");

                System.out.println("Get Block: " + tokens.get(1));
                Block block = null;
                try {
                    block = ledger.getBlock(Integer.parseInt(tokens.get(1)));
                } catch (LedgerException e) {
                    System.out.println("Failed due to: " + e.getReason());
                    break;
                }

                System.out.println("Block Number: " + block.getBlockNumber() + " "
                        + "Hash: " + block.getHash() + " " + "Previous Hash: " + block.getPreviousHash()
                );

                for(Transaction transaction: block.getTransactionList()){
                    System.out.println(transaction.toString());
                }

            }
            case "get-transaction" -> {

                if(tokens.size() != 2)
                    throw new CommandProcessorException("get-transaction", "Missing Arguments");

                System.out.println("Get Transaction: " + tokens.get(1));
                Transaction transaction = ledger.getTransaction(tokens.get(1));

                System.out.println("Transaction ID: " + transaction.getTransactionId() + " "
                        + "Amount: " + transaction.getAmount() + " " + "Fee: "
                        + transaction.getFee() + " " + "Note: " + transaction.getNote() + " " + "Payer: "
                        + transaction.getPayer().getAddress() + " " + "Receiver: "
                        + transaction.getReceiver().getAddress()
                );
            }
            case "validate" -> {
                System.out.print("Validate: ");
                try {
                    ledger.validate();
                    System.out.println("Valid");
                } catch (LedgerException e) {
                    System.out.println("Failed due to: " + e.getReason());
                }

            }
            default ->  {
                throw new CommandProcessorException(tokens.get(0), "Invalid Command");

            }

        }

    }

    /**
     * Process File from the command line
     */
    public void processCommandFile(String fileName){
        List<String> tokens = new ArrayList<>();
        AtomicInteger atomicInteger = new AtomicInteger(0);

        //Process all the lines in the file
        try (Stream<String> stream = Files.lines(Paths.get(fileName))) {
            processCommandStream(stream, atomicInteger);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Process command stream
     */
    public void processCommandStream(Stream<String> stream, AtomicInteger lineCounter) {
        stream.forEach(line -> {
            try {
                lineCounter.getAndIncrement();
                if(!line.trim().startsWith("#") && line.trim().length() != 0) {
                    processCommand(line);
                }
            } catch (CommandProcessorException e) {
                e.setLineNumber(lineCounter.get());
                System.out.println("Failed due to: " + e.getReason() + " for Command: " + e.getCommand()
                        + " On Line Number: " + e.getLineNumber());
            }
        });
    }

    /**
     * Process command stream from an input stream
     */
    public void processCommandInputStream(InputStream inputStream) {
        AtomicInteger atomicInteger = new AtomicInteger(0);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            processCommandStream(reader.lines(), atomicInteger);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
