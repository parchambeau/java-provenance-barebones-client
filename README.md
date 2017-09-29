# Console Provenance Client

## Getting Started
This application demonstrates Proof of Provenance using the Mastercard Core Blockchain API. To get started you should take the following steps 
 * Install protoc on your workstation (Google's protocol buffer compiler)
 * Clone this repository
 * Goto Mastercard Developers and create a Mastercard Blockchain project (note this is currently a private API and you may need to request access). You will be taken through the wizard to create a node. You must provide an APP_ID and a protocol buffer definition i.e. message.proto.
    * You will receive a p12 file and a consumer key from Mastercard Developers for your project.
 * Execute the following commands
   * `mvn package`
   * `java -jar target/console-provenance-client-0.0.1-SNAPSHOT.one-jar.jar -kp <path/to/p12> -ck '<consumer key>`

## Usage

### Create Users

Create a new user marked as an issuing authority. Products can be created and issued to the blockchain only by users marked as authorities.

Create other users to represent merchants and consumers.

### Create a Product

Create a product and associate it with the issuing authority user.
The product can then be transferred to other users.

### Authenticate a Product

Product ownership can be verified by specifying the product and user to verify ownership against.

## Caution

It may be necessary to manually create a node instance on the blockchain if it doesn't get created on the developer portal. This can be done from the app by selecting option 8. 