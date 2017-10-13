package com.mastercard.blockchain;

import PAXM.Message;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.mastercard.api.blockchain.App;
import com.mastercard.api.blockchain.Block;
import com.mastercard.api.blockchain.Node;
import com.mastercard.api.blockchain.TransactionEntry;
import com.mastercard.api.core.ApiConfig;
import com.mastercard.api.core.exception.ApiException;
import com.mastercard.api.core.model.RequestMap;
import com.mastercard.api.core.model.ResourceList;
import com.mastercard.api.core.security.oauth.OAuthAuthentication;
import com.mastercard.blockchain.api.ChainUtil;
import com.mastercard.blockchain.data.ProductDataUtil;
import com.mastercard.blockchain.data.TransferDataUtil;
import com.mastercard.blockchain.data.UserDataUtil;
import com.mastercard.blockchain.model.*;
import org.apache.commons.cli.*;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2017 Mastercard. All Rights Reserved.
 */
public class ConsoleProvenanceClientApplication {

    private String ENCODING = "hex";
    private String APP_ID = getAppIdFromProtoBuffer();

    private String lastHash = "";
    private String lastSlot = "";
    private String encodedProtoDef = "";
    private String appId;

    private UserDataUtil userDataUtil = new UserDataUtil();
    private ProductDataUtil productDataUtil = new ProductDataUtil();
    private TransferDataUtil transferDataUtil = new TransferDataUtil();

    public static void main(String... args) throws Exception {
        CommandLineParser parser = new DefaultParser();
        Options options = createOptions();
        CommandLine cmd = parser.parse(options, args);

        new ConsoleProvenanceClientApplication().start(cmd, options);
    }

    private void start(CommandLine cmd, Options options) throws FileNotFoundException {
        System.out.println(readResourceToString("/help.txt"));
        encodedProtoDef = encode(readResourceToString("/message.proto").getBytes(), "base64");

        System.out.println();
        initApi(cmd);
        updateNode(appId, true);
        menu(cmd, options);
    }

    private void menu(CommandLine cmd, Options options) {
        final String quit = "0";
        String option = "";
        while (!option.equals(quit)) {
            printHeading("MENU");
            System.out.println("1. Create user");
            System.out.println("2. List users");
            System.out.println("3. Create product");
            System.out.println("4. List products");
            System.out.println("5. Authenticate product");
            System.out.println("6. Transfer product");
            System.out.println("7. Print Command Line Options");
            System.out.println("8. Provision instance");
            System.out.println("9. Reset datastore");
            System.out.println("10. Update instance");
            System.out.println(quit + ". Quit");
            option = captureInput("Option", quit);
            if (option.equals("1")) {
                createUser();
            } else if (option.equals("2")) {
                listUsers();
            } else if (option.equals("3")) {
                createProduct();
            } else if (option.equals("4")) {
                listProducts();
            } else if (option.equals("5")) {
                authenticateProduct();
            } else if (option.equals("6")) {
                transferProduct();
            } else if (option.equals("7")) {
                printHeading("COMMAND LINE OPTIONS");
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("java -jar <jarfile>", options);
                captureInput("(press return to continue)", null);
            } else if (option.equals("8")) {
                provisionInstance();
            } else if (option.equals("9")) {
                // reset data structures in memory and on file
                productDataUtil.clear();
                userDataUtil.clear();
                transferDataUtil.clear();
                System.out.println("Data Store Cleared.");
            } else if (option.equals("10")) {
                printHeading("UPDATE NODE");
                updateNode(appId, false);
            } else if (option.equals(quit)) {
                System.out.println("Goodbye");
            } else {
                System.out.println("Unrecognised option");
            }
        }
    }

    private void updateNode(String appId, boolean silently) {
        String protoPath = captureInput("Protocol Definition Path", "/message.proto");

        try {
            RequestMap map = new RequestMap();
            map.set("id", appId);
            map.set("name", appId);
            map.set("description", "");
            map.set("version", 0);
            map.set("definition.format", "proto3");
            map.set("definition.encoding", "base64");
            map.set("definition.messages", Base64.getEncoder().encodeToString(readResourceToString(protoPath).replace("PAXM", appId).getBytes()));
            new App(map).update();
            System.out.println("Node updated");
            App app = App.read(appId);
            if (!silently) {
                JSONObject definition = (JSONObject) app.get("definition");
                System.out.println("New Format: " + definition.get("format"));
                System.out.println("New Encoding: " + definition.get("encoding"));
                System.out.println("New Messages: " + definition.get("messages"));
            }
        } catch (ApiException e) {
            System.err.println("API Exception " + e.getMessage());
        }
        if (!silently) {
            captureInput("(press return to continue)", null);
        }
    }

    private void createUser() {
        printHeading("Create User");
        String name = captureInput("Name", "John Doe");
        boolean isAuthority = captureBoolean("Is Authority", false);

        User user = new User();
        user.setAddress(generateChainAddress());
        user.setName(name);
        user.setAuthority(isAuthority);

        System.out.println("Created new user: " + user.toString());
        userDataUtil.add(user);

        captureInput("(press return to continue)", null);
    }

    private void createProduct() {
        printHeading("Create Product");
        System.out.println(productDataUtil.list());
        String name = captureInput("Name", "Diamond");
        String manufacturerReference = captureInput("Manufacturer Reference", "");
        System.out.println(userDataUtil.list());
        String ownerIdx = captureInput("Select Authority", null);

        User user = getUser(Integer.parseInt(ownerIdx));
        if (user == null) {
            printError("No user found");
        } else if (!user.isAuthority()) {
            printError("Please select an authority");
        } else {

            Product product = new Product();
            product.setAddress(generateChainAddress());
            product.setName(name);
            product.setManufacturerReference(manufacturerReference);

            Message.Product.Builder productBuilder = Message.Product.newBuilder();
            Message.Product productMessage  = productBuilder
                    .setProductAddress(product.getAddress().getIdentity())
                    .setManufacturerLocatorId(ByteString.copyFrom(product.getManufacturerReference().getBytes(StandardCharsets.UTF_8)))
                    .build();

            Message.ProductIssuance.Builder productIssuanceBuilder = Message.ProductIssuance.newBuilder()
                    .setTimestamp(System.currentTimeMillis())
                    .setIssuerAddress(user.getAddress().getIdentity())
                    .addProduct(productMessage);

            productIssuanceBuilder.setIssuerSignature(ByteString.copyFrom(ChainUtil.sign(productIssuanceBuilder.build().toByteArray(), user.getAddress().getPrivateKey())));
            Message.ProductIssuance productIssuanceMessage = productIssuanceBuilder.build();

            try {
                Message.PaxmMessage paxmMessage = Message.PaxmMessage.newBuilder()
                        .setType(Message.MessageType.ISSUANCE)
                        .setPayload(productIssuanceMessage.toByteString())
                        .build();

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                paxmMessage.writeTo(baos);
                String encoded = encode(baos.toByteArray(), ENCODING);
                RequestMap request = new RequestMap();
                request.put("app", appId);
                request.put("encoding", ENCODING);
                request.put("value", encoded);
                TransactionEntry response = TransactionEntry.create(request);
                printEntry(response);

                waitForConfirmation(response.get("hash").toString(), response.get("slot").toString(), response.get("status").toString());

                product.setHash(response.get("hash").toString());
                productDataUtil.add(product);

                System.out.println("Created new product: " + product.toString());
            } catch (IOException e) {
                System.err.println(e.getMessage());
            } catch (ApiException e) {
                System.err.println(e.getMessage());
            } catch (InterruptedException e) {
                System.err.println(e.getMessage());
            }
        }
        captureInput("(press return to continue)", null);
    }

    private void transferProduct() {
        printHeading("Transfer Product");
        System.out.println(productDataUtil.list());
        String productIdx = captureInput("Select product", null);
        System.out.println(userDataUtil.list());
        String newOwnerIdx = captureInput("Select new owner", null);

        Product product = getProduct(Integer.parseInt(productIdx));
        if (product == null) {
            printError("No product found");
        }

        User user = getUser(Integer.parseInt(newOwnerIdx));
        if (user == null) {
            printError("No user found");
        }

        User currentOwner = null;
        String previousTransferHash = null;
        List<Transfer> transfers = product.getTransfers();
        Message.ProductTransfer.Builder productTransferBuilder = null;

        try{
            if(transfers.isEmpty()){
                TransactionEntry transactionEntry = retrieveEntry(product.getHash());
                String value = (String) transactionEntry.get("value");
                Message.PaxmMessage message = Message.PaxmMessage.parseFrom(decode(value, ENCODING));
                System.out.println("type: " + message.getType().name());
                Message.ProductIssuance issuanceMsg = Message.ProductIssuance.parseFrom(message.getPayload());
                currentOwner = userDataUtil.getByAddress(issuanceMsg.getIssuerAddress());
                previousTransferHash = product.getHash();
            }
            else{
                Transfer lastTransfer = transfers.get(transfers.size() - 1);
                TransactionEntry transactionEntry = retrieveEntry(lastTransfer.getHash());
                String transferValue = (String) transactionEntry.get("value");
                Message.PaxmMessage transferMessage = Message.PaxmMessage.parseFrom(decode(transferValue, ENCODING));
                System.out.println("type: " + transferMessage .getType().name());
                Message.ProductTransfer innerTransferMsg = Message.ProductTransfer.parseFrom(transferMessage.getPayload());
                currentOwner = userDataUtil.getByAddress(innerTransferMsg.getNextOwnerAddress());
                previousTransferHash = lastTransfer.getHash();
            }

            productTransferBuilder = Message.ProductTransfer.newBuilder()
                    .setTimestamp(System.currentTimeMillis())
                    .setPreviousTransfer(ByteString.copyFrom(previousTransferHash.getBytes(StandardCharsets.UTF_8)))
                    .setProductAddress(product.getAddress().getIdentity())
                    .setNextOwnerAddress(user.getAddress().getIdentity());

        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
            return;
        } catch (DecoderException e) {
            e.printStackTrace();
            return;
        }

        productTransferBuilder
                .setOwnerSignature(ByteString.copyFrom(ChainUtil.sign(productTransferBuilder.build().toByteArray(), currentOwner.getAddress().getPrivateKey())))
                .setOwnerPublicKey(ByteString.copyFrom(currentOwner.getAddress().getPublicKey()));

        Message.ProductTransfer productTransferMessage = productTransferBuilder.build();

        try {
            Message.PaxmMessage paxmMessage = Message.PaxmMessage.newBuilder()
                    .setType(Message.MessageType.TRANSFER)
                    .setPayload(productTransferMessage.toByteString())
                    .build();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            paxmMessage.writeTo(baos);
            String encoded = encode(baos.toByteArray(), ENCODING);
            RequestMap request = new RequestMap();
            request.put("app", appId);
            request.put("encoding", ENCODING);
            request.put("value", encoded);
            TransactionEntry response = TransactionEntry.create(request);
            printEntry(response);

            String entryHash = response.get("hash").toString();
            waitForConfirmation(entryHash, response.get("slot").toString(), response.get("status").toString());

            Transfer transfer = new Transfer();
            transfer.setHash(entryHash);
            transfer.setProductAddress(product.getAddress().getIdentity());
            transfer.setSlot(response.get("slot").toString());
            transferDataUtil.add(transfer);

            product.setTransfers(transferDataUtil.getListByProduct(product));
            productDataUtil.saveToFile();

            System.out.println(String.format("%s transferred from %s to %s", product.getName(), currentOwner.getName(), user.getName()));
        } catch (IOException e) {
            System.err.println(e.getMessage());
        } catch (ApiException e) {
            System.err.println(e.getMessage());
        } catch (InterruptedException e) {
            System.err.println(e.getMessage());
        }

        captureInput("(press return to continue)", null);
    }

    private void waitForConfirmation(String hash, String slot, String status) throws InterruptedException {
        int interval = 3000;
        String currentStatus = status;
        while (!"confirmed".equals(currentStatus)) {
            Thread.sleep(interval);
            System.out.println("Checking confirmation status blockchain entry...");
            TransactionEntry transactionEntry = retrieveEntry(hash);
            printEntry(transactionEntry);
            currentStatus = transactionEntry.get("status").toString();
        }
    }

    private void printError(String message) {
        System.err.println(message);
    }

    private User getUser(int userIdx) {
        return userDataUtil.get(userIdx);
    }

    private Product getProduct(int productIdx) {
        return productDataUtil.get(productIdx);
    }

    private ChainAddress generateChainAddress() {
        return new ChainAddress(ChainUtil.generateAddress(AddressPrefix.Entity));
    }

    private void provisionInstance() {
        printHeading("PROVISION INSTANCE");

        try {
            RequestMap map = new RequestMap();
            map.set("network", "Z0NE");
            map.set("application.name", appId);
            map.set("application.description", "");
            map.set("application.version", 0);
            map.set("application.definition.format", "proto3");
            map.set("application.definition.encoding", "base64");
            map.set("application.definition.messages", encodedProtoDef);
            Node response = Node.provision(map);

            System.out.println("address-->" + response.get("address")); // address-->CNkNVuVnQ4WigadrQKcNuTa1JkAJFWF9S8
            System.out.println("authority-->" + response.get("authority")); // authority-->ShgddyMCV6oBL7putekmJkYzXbGuoyggA8
            System.out.println("type-->" + response.get("type")); // type-->customer
        } catch (ApiException e) {
            System.err.println(e.getMessage());
        }
        captureInput("(press return to continue)", null);
    }

    private TransactionEntry retrieveEntry(String hash) {
        System.out.println("Retrieving entry for " + hash);
        try {
            RequestMap request = new RequestMap();
            request.put("hash", hash);
            return TransactionEntry.read("", request);
        } catch (ApiException e) {
            System.err.println(e.getMessage());
        }

        return null;
    }

    private void printEntry(TransactionEntry response) {
        System.out.println("Hash: " + response.get("hash").toString());
        System.out.println("Slot: " + response.get("slot").toString());
        System.out.println("Status: " + response.get("status").toString());
        if (response.containsKey("value")) {
            String hexEncoded = response.get("value").toString();
            System.out.println("Value: " + hexEncoded);
            try {
                Message.PaxmMessage message = Message.PaxmMessage.parseFrom(decode(hexEncoded, "hex"));
                System.out.println("Decoded Value: " + message.toString());
            } catch (DecoderException | InvalidProtocolBufferException e) {
                System.err.println("Unable to decode: " + hexEncoded);
            }
        }
    }

    private void retrieveBlock() {
        printHeading("RETRIEVE BLOCK");
        String slot = captureInput("Block Id", lastSlot);
        try {
            RequestMap request = new RequestMap();
            request.put("id", slot);
            Block response = Block.read(slot);
            printBlock(response);
        } catch (ApiException e) {
            if (e.getHttpStatus() == 404) {
                System.err.println("Block not found (it may not have been written yet!)");
            } else {
                System.err.println(e.getMessage());
            }
        }
        captureInput("(press return to continue)", null);
    }

    private void retrieveLastConfirmedBlock() {
        printHeading("RETRIEVE LAST BLOCK");
        try {
            ResourceList<Block> response = Block.list();
            if (response.getList().size() > 0) {
                printBlock(response.getList().get(0));
            } else {
                System.err.println("No confirmed blocks returned!");
            }
        } catch (ApiException e) {
            if (e.getHttpStatus() == 404) {
                System.err.println("Block not found (it may not have been written yet!)");
            } else {
                System.err.println(e.getMessage());
            }
        }
        captureInput("(press return to continue)", null);
    }

    private void printBlock(Block response) {
        System.out.println("Hash: " + response.get("hash").toString());
        System.out.println("Slot: " + response.get("slot").toString());
        System.out.println("Version: " + response.get("version").toString());
        System.out.println("Previous Block: " + response.get("previous_block").toString());
        System.out.println("Nonce: " + response.get("nonce").toString());
        System.out.println("Authority: " + response.get("authority").toString());
        System.out.println("Signature: " + response.get("signature").toString());
        JSONArray partitions = (JSONArray) response.get("partitions");
        if (partitions.size() > 0) {
            for (int i = 0; i < partitions.size(); i++) {
                System.out.println("Partition[" + i + "]: ");
                JSONObject o = JSONObject.class.cast(partitions.get(i));
                System.out.println("\tApplication: " + o.get("application"));
                System.out.println("\tMerkle Root: " + o.get("merkle_root"));
                System.out.println("\tEntry Count: " + o.get("entry_count"));
                JSONArray entries = (JSONArray) o.get("entries");
                if (entries.size() > 0) {
                    for (int j = 0; j < entries.size(); j++) {
                        String entry = String.class.cast(entries.get(j));
                        System.out.println("\t\tEntries[" + j + "]: " + entry);
                    }
                } else {
                    System.out.println("Entries: NONE");
                }
            }
        } else {
            System.out.println("Partitions: NONE");
        }
    }

    private void initApi(CommandLine cmd) throws FileNotFoundException {
        // TODO: 9/29/17 to setup auto commands of the keystore
        // TODO: 9/29/17 to check whether the user wants to use the default settings
        String keystorePath = captureInputFile("Keystore", cmd.getOptionValue("keystorePath", ""));
        String storePass = captureInput("Keystore Password", cmd.getOptionValue("storePass", "keystorepassword"));
        String consumerKey = captureInput("Consumer Key", cmd.getOptionValue("consumerKey", ""));
        String keyAlias = captureInput("Key Alias", cmd.getOptionValue("keyAlias", "keyalias"));
        appId = captureInput("Team Name (e.g. TM01):", "PAXM");

        ApiConfig.setAuthentication(
                new OAuthAuthentication(
                        consumerKey,
                        new FileInputStream(keystorePath),
                        keyAlias,
                        storePass));
        ApiConfig.setDebug(cmd.hasOption("verbosity"));
        ApiConfig.setSandbox(true);
    }

    private String captureInputFile(String question, String defaultAnswer) {
        boolean noFile = true;
        String keystorePath = null;
        while (noFile) {
            keystorePath = captureInput(question, defaultAnswer);
            keystorePath = keystorePath.replaceFirst("^~/", System.getProperty("user.home") + "/");
            if (Files.notExists(Paths.get(keystorePath))) {
                System.out.println("File Not Found");
            } else {
                noFile = false;
            }
        }
        return keystorePath;
    }

    private String captureInput(String question, String defaultAnswer) {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        if (defaultAnswer == null) {
            System.out.print(question + ": ");
        } else {
            System.out.print(question + " [" + defaultAnswer + "]: ");
        }
        String s;
        try {
            s = br.readLine();
            if (s == null || "".equals(s)) {
                s = defaultAnswer;
            }
        } catch (IOException e) {
            s = defaultAnswer;
        }
        return s;
    }

    private boolean captureBoolean(String question, boolean defaultAnswer) {
        String yes = "yes";
        String no = "no";
        String prompt = defaultAnswer ? String.format("%s / %s", yes.toUpperCase(), no) : String.format("%s / %s", yes, no.toUpperCase());

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.print(question + " [" + prompt + "]: ");

        boolean choice = defaultAnswer;
        String s;
        try {
            s = br.readLine();
            if (s == null || "".equals(s)) {
                choice = defaultAnswer;
            } else {
                s = s.toLowerCase();

                if (s.equals(yes)) {
                    choice = true;
                }

                if (s.equals(no)) {
                    choice = false;
                }
            }
        } catch (IOException e) {
            choice = defaultAnswer;
        }
        return choice;
    }

    private void printHeading(String heading) {
        System.out.println("============ " + heading + " ============");
    }

    private static String readResourceToString(String path) {
        return new BufferedReader(new InputStreamReader(ConsoleProvenanceClientApplication.class.getResourceAsStream(path)))
                .lines().collect(Collectors.joining("\n"));
    }

    private String encode(byte[] bytes, String encoding) {
        if (encoding.equals("hex")) {
            return Hex.encodeHexString(bytes);
        } else {
            return Base64.getEncoder().encodeToString(bytes);
        }
    }

    private byte[] decode(String encoded, String encoding) throws DecoderException {
        if (encoding.equals("hex")) {
            return Hex.decodeHex(encoded.toCharArray());
        } else {
            return Base64.getDecoder().decode(encoded);
        }
    }

    public static String getAppIdFromProtoBuffer() {
        String protoBuf = readResourceToString("/message.proto");
        Pattern pattern = Pattern.compile("package\\s(.[A-Za-z0-9]+);", Pattern.MULTILINE);
        Matcher m = pattern.matcher(protoBuf);
        if (m.find()) {
            return m.group(1);
        }
        return "";
    }

    private static Options createOptions() {
        Options options = new Options();

        options.addOption("ck", "consumerKey", true, "consumer key (mastercard developers)");
        options.addOption("kp", "keystorePath", true, "the path to your keystore (mastercard developers)");
        options.addOption("ka", "keyAlias", true, "key alias (mastercard developers)");
        options.addOption("sp", "storePass", true, "keystore password (mastercard developers)");
        options.addOption("v", "verbosity", false, "log mastercard developers sdk to console");

        return options;
    }

    private void listUsers() {
        printHeading("User Listing");
        printHeading("***************");
        System.out.println(userDataUtil.list());
    }

    private void listProducts() {
        printHeading("Product Listing");
        printHeading("***************");
        System.out.println(productDataUtil.list());
    }

    private void authenticateProduct() {
        printHeading("Select Product to authenticate");
        System.out.println(productDataUtil.list());
        String productIndex = captureInput("Product Index", "1");
        printHeading("Select User that should be owner");
        System.out.println(userDataUtil.list());
        String userIndex = captureInput("User Index", "1");

        Product product = productDataUtil.get(Integer.parseInt(productIndex));
        User user = userDataUtil.get(Integer.parseInt(userIndex));

        if (product != null && user != null) {
            try {
                TransactionEntry readEntryResponse = retrieveEntry(product.getHash());
                String value = (String) readEntryResponse.get("value");
                Message.PaxmMessage message = Message.PaxmMessage.parseFrom(decode(value, ENCODING));
                Message.ProductIssuance issuanceMsg = Message.ProductIssuance.parseFrom(message.getPayload());
                System.out.println("Product Issuance");
                System.out.println("Timestamp: " + issuanceMsg.getTimestamp());
                System.out.println("Issuer Address: " + issuanceMsg.getIssuerAddress());
                System.out.println("Product Manufacturer Reference: " + new String(issuanceMsg.getProduct(0).getManufacturerLocatorId().toByteArray()));
                System.out.println("Issuer Signature: " + encode(issuanceMsg.getIssuerSignature().toByteArray(), ENCODING));

                String verificationMessage = null;
                List<Transfer> transfers = product.getTransfers();
                if(transfers.isEmpty()){
                    verificationMessage = user.getAddress().getIdentity().equals(issuanceMsg.getIssuerAddress()) ? String.format("%s belongs to %s", product.getName(), user.getName()) : String.format("%s does not belong to %s", product.getName(), user.getName());
                }
                else{
                    int transferTimes = 0;
                    Message.ProductTransfer innerTransferMsg = null;
                    for (Transfer transfer : product.getTransfers()) {
                        TransactionEntry transferEntryResponse = retrieveEntry(transfer.getHash());
                        String transferValue = (String) transferEntryResponse.get("value");
                        Message.PaxmMessage transferMessage = Message.PaxmMessage.parseFrom(decode(transferValue, ENCODING));
                        innerTransferMsg = Message.ProductTransfer.parseFrom(transferMessage.getPayload());
                        System.out.println("Product Transfer " + ++transferTimes);
                        System.out.println("Product Address: " + innerTransferMsg.getProductAddress());
                        System.out.println("Next Owner Address: " + innerTransferMsg.getNextOwnerAddress());
                        System.out.println("Owner Signature: " + encode(innerTransferMsg.getOwnerSignature().toByteArray(), ENCODING));
                    }
                    if (innerTransferMsg != null && innerTransferMsg.getNextOwnerAddress().equals(user.getAddress().getIdentity())) {
                        verificationMessage = String.format("%s belongs to %s", product.getName(), user.getName());
                    }
                    else{
                        verificationMessage = String.format("%s does not belong to %s", product.getName(), user.getName());
                    }
                }

                System.out.println(verificationMessage);

            } catch (Exception e) {
                e.printStackTrace();
            }


        } else {
            printError("Invalid Product or User");
        }

    }
}
