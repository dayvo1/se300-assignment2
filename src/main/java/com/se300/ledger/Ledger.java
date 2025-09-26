package com.se300.ledger;

import java.util.*;
import static java.util.Map.*;

/**
 * Ledger Class representing simple implementation of Blockchain
 *
 * @author  Sergey L. Sundukovskiy
 * @version 1.0
 * @since   2025-09-25
 */
public class Ledger {
    private String name;
    private String description;
    private String seed;
    private static NavigableMap <Integer,Block> blockMap;
    private static Block uncommittedBlock;

    private static Ledger ledger;

    // Initialize genesis block and the account list
    static {
        blockMap = new TreeMap<>();
        uncommittedBlock = new Block(1, "");
        uncommittedBlock.addAccount("master", new Account("master", Integer.MAX_VALUE));
    }

    /**
     * Create singleton of the Ledger
     * @param name
     * @param description
     * @param seed
     * @return
     */
    public static synchronized Ledger getInstance(String name, String description, String seed) {
        if (ledger == null) {
            ledger = new Ledger(name, description, seed);
        }
        return ledger;
    }

    /**
     * Private Ledger Constructor
     * @param name
     * @param description
     * @param seed
     */
    private Ledger(String name, String description, String seed) {
        this.name = name;
        this.description = description;
        this.seed = seed;
    }

    /**
     * Getter method for the name of the Ledger
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * Setter Method for the name of the Ledger
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Getter Method for Ledger description
     * @return String
     */
    public String getDescription() {
        return description;
    }

    /**
     * Setter Method for Description
     * @param description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Getter Method for the seed
     * @return String
     */
    public String getSeed() {
        return seed;
    }

    /**
     * Setter Method for the seed
     * @param seed
     */
    public void setSeed(String seed) {
        this.seed = seed;
    }

    /**
     * Method for creating accounts in the blockchain
     * @param address
     * @return Account representing account in the Blockchain
     */
    public Account createAccount(String address) throws LedgerException {

        if(uncommittedBlock.getAccount(address) != null){
            throw new LedgerException("Create Account", "Account Already Exists");
        }

        Account account = new Account(address, 0);
        uncommittedBlock.addAccount(address, account);
        return account;
    }

    /**
     * Method implementing core functionality of the Blockchain by handling given transaction
     * @param transaction
     * @return String representing transaction id
     * @throws LedgerException
     */
    public synchronized String processTransaction(Transaction transaction) throws LedgerException {

        //Check for transaction specification conditions
        if(transaction.getAmount() < 0 || transaction.getAmount() > Integer.MAX_VALUE ){
            throw new LedgerException("Process Transaction", "Transaction Amount Is Out of Range");
        } else if (transaction.getFee() < 10) {
            throw new LedgerException("Process Transaction", "Transaction Fee Must Be Greater Than 10");
        } else if (transaction.getNote().length() > 1024){
            throw new LedgerException("Process Transaction", "Note Length Must Be Less Than 1024 Chars");
        }

        if(ledger.getTransaction(transaction.getTransactionId()) != null){
            throw new LedgerException("Process Transaction", "Transaction Id Must Be Unique");
        }

        Account tempPayerAccount = transaction.getPayer();
        Account tempReceiverAccount = transaction.getReceiver();

        if(transaction.getPayer().getBalance() < (transaction.getAmount() + transaction.getFee()))
            throw new LedgerException("Process Transaction", "Payer Does Not Have Required Funds");

        //Deduct balance of the payer
        tempPayerAccount.setBalance(tempPayerAccount.getBalance()
                - transaction.getAmount() - transaction.getFee());
        //Increase balance of the receiver
        tempReceiverAccount.setBalance(tempReceiverAccount.getBalance() + transaction.getAmount());

        uncommittedBlock.getTransactionList().add(transaction);

        //Check to see if account blocked has reached max size
        if (uncommittedBlock.getTransactionList().size() == 10){

            List<String> tempTxList = new ArrayList<>();
            tempTxList.add(seed);

            //Loop through the list of transaction to get the hash
            for( Transaction tempTx : uncommittedBlock.getTransactionList()){
                tempTxList.add(tempTx.toString());
            }

            MerkleTrees merkleTrees = new MerkleTrees(tempTxList);
            merkleTrees.merkle_tree();
            uncommittedBlock.setHash(merkleTrees.getRoot());

            //Commit uncommitted block
            blockMap.put(uncommittedBlock.getBlockNumber(), uncommittedBlock);

            //Get committed block
            Block committedBlock = blockMap.lastEntry().getValue();
            Map<String,Account> accountMap = committedBlock.getAccountBalanceMap();

            //Get all the accounts
            List<Account> accountList = new ArrayList<Account>(accountMap.values());

            //Create next block
            uncommittedBlock = new Block(uncommittedBlock.getBlockNumber() + 1,
                    committedBlock.getHash());

            //Replicate accounts
            for (Account account : accountList) {
                Account tempAccount = (Account) account.clone();
                uncommittedBlock.addAccount(tempAccount.getAddress(), tempAccount);
            }

            //Link to previous block
            uncommittedBlock.setPreviousBlock(committedBlock);
        }

        return transaction.getTransactionId();
    }

    /**
     * Get Account balance by address
     * @param address
     * @return Integer representing balance of the Account
     * @throws LedgerException
     */
    public Integer getAccountBalance(String address) throws LedgerException {

        if(blockMap.isEmpty()){
            throw new LedgerException("Get Account Balance", "Account Is Not Committed to a Block");
        }

        Block block = blockMap.lastEntry().getValue();
        Account account = block.getAccount(address);

        if (account == null)
            throw new LedgerException("Get Account Balance", "Account Does Not Exist");
        else
            return account.getBalance();
    }

    /**
     * Get all Account balances that are part of the Blockchain
     * @return Map representing Accounts and balances
     */
    public Map<String,Integer> getAccountBalances(){

        if(blockMap.isEmpty())
            return null;

        Block committedBlock = blockMap.lastEntry().getValue();
        Map<String,Account> accountMap = committedBlock.getAccountBalanceMap();

        Map<String, Integer> balances = new HashMap<>();
        List<Account> accountList = new ArrayList<>(accountMap.values());

        for (Account account : accountList) {
            balances.put(account.getAddress(), account.getBalance());
        }

        return balances;
    }

    /**
     * Get Block by id
     * @param blockNumber
     * @return Block or Null
     */
    public Block getBlock (Integer blockNumber) throws LedgerException {
        Block block = blockMap.get(blockNumber);
        if(block == null){
            throw new LedgerException("Get Block", "Block Does Not Exist");
        }
        return block;
    }

    /**
     * Get Transaction by id
     * @param transactionId
     * @return Transaction or Null
     */
    public Transaction getTransaction (String transactionId){

        for ( Entry mapElement : blockMap.entrySet()) {

            // Finding specific transactions in the committed blocks
            Block tempBlock = (Block) mapElement.getValue();
            for (Transaction transaction : tempBlock.getTransactionList()){
                if(transaction.getTransactionId().equals(transactionId)){
                    return transaction;
                }
            }
        }
        // Finding specific transactions in the uncommitted block
        for (Transaction transaction : uncommittedBlock.getTransactionList()){
            if(transaction.getTransactionId().equals(transactionId)){
                return transaction;
            }
        }
        return null;
    }

    /**
     * Get number of Blocks in the Blockchain
     * @return int representing number of blocks committed to Blockchain
     */
    public int getNumberOfBlocks(){
        return blockMap.size();
    }

    /**
     * Method for validating Blockchain.
     * Check each block for Hash consistency
     * Check each block for Transaction count
     * Check account balances against the total
     */
    public void validate() throws LedgerException {

        if(blockMap.isEmpty()){
            throw new LedgerException("Validate", "No Block Has Been Committed");
        }

        Block committedBlock = blockMap.lastEntry().getValue();
        Map<String,Account> accountMap = committedBlock.getAccountBalanceMap();
        List<Account> accountList = new ArrayList<>(accountMap.values());

        int totalBalance = 0;
        for (Account account : accountList) {
            totalBalance += account.getBalance();
        }

        int fees = 0;
        String hash;
        for(Integer key : blockMap.keySet()){
            Block block = blockMap.get(key);

            //Check for Hash Consistency
            if(block.getBlockNumber() != 1)
                if(!block.getPreviousHash().equals(block.getPreviousBlock().getHash())){
                    throw new LedgerException("Validate", "Hash Is Inconsistent: "
                            + block.getBlockNumber());
            }

            //Check for Transaction Count
            if(block.getTransactionList().size() != 10){
                throw new LedgerException("Validate", "Transaction Count Is Not 10 In Block: "
                        + block.getBlockNumber());
            }

            for(Transaction transaction : block.getTransactionList()){
                fees += transaction.getFee();
            }
        }

        int adjustedBalance = totalBalance + fees;

        //Check for account balances against the total
        if(adjustedBalance != Integer.MAX_VALUE){
            throw new LedgerException("Validate", "Balance Does Not Add Up");
        }

    }

    /**
     * Helper method for CommandProcessor
     * @return current block we are working with
     */
    public Block getUncommittedBlock(){
        return uncommittedBlock;
    }

    /**
     * Helper method allowing reset the state of the Ledger
     */
    public synchronized void reset(){
        blockMap = new TreeMap<>();
        uncommittedBlock = new Block(1, "");
        uncommittedBlock.addAccount("master", new Account("master", Integer.MAX_VALUE));
    }
}
