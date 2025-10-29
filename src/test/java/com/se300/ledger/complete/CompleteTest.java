package com.se300.ledger.complete;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.Assumptions.assumingThat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.se300.ledger.Account;
import com.se300.ledger.Block;
import com.se300.ledger.Ledger;
import com.se300.ledger.LedgerException;
import com.se300.ledger.MerkleTrees;
import com.se300.ledger.Transaction;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CompleteTest {

    /* TODO: The following
     * 1. Achieve 100% Test Coverage
     * 2. Produce/Print Identical Results to Command Line DriverTest
     * 3. Produce Quality Report
     */

    private static Ledger ledger;
    private static int testCounter = 0;

    /**
     * Setup method called once before all tests
     */
    @BeforeAll
    static void setUpAll() {
        System.out.println("=== Starting CompleteTest Suite ===");
        ledger = Ledger.getInstance("TestLedger", "Test Blockchain", "test-seed");
    }

    /**
     * Teardown method called once after all tests
     */
    @AfterAll
    static void tearDownAll() {
        System.out.println("=== Completed CompleteTest Suite ===");
        System.out.println("Total tests executed: " + testCounter);
    }

    /**
     * Setup method called before each test
     */
    @BeforeEach
    void setUp() {
        testCounter++;
        ledger.reset();
        System.out.println("Starting test #" + testCounter);
    }

    /**
     * Teardown method called after each test
     */
    @AfterEach
    void tearDown() {
        System.out.println("Finished test #" + testCounter);
    }


    @ParameterizedTest
    @ValueSource(strings = {"account1", "account2", "account3", "alice", "bob"})
    @DisplayName("Parameterized Test: Create accounts with different names")
    @Order(1)
    void parameterizedValueSourcesTest(String value) throws LedgerException {
        Account account = ledger.createAccount(value);
        assertNotNull(account);
        assertEquals(value, account.getAddress());
        assertEquals(0, account.getBalance());
    }

    @ParameterizedTest
    @CsvSource({
        "alice, 10, 1000",
        "bob, 20, 2000",
        "charlie, 15, 3000",
        "david, 30, 4000",
    })
    @DisplayName("Parameterized Test: Create accounts and recieve amounts using CSV source")
    @Order(2)
    void parameterizedComplexSourcesTest(String str, int fee, int num) throws LedgerException {
        Account account = ledger.createAccount(str);
        Account master = ledger.getUncommittedBlock().getAccount("master");
        Transaction tx = new Transaction("csv test: " + str, num, fee, "funding", master, account);

        assertDoesNotThrow(() -> {
            ledger.processTransaction(tx);
        });
        System.out.println(tx.getTransactionId());
    }


    @RepeatedTest(3)
    @DisplayName("Repeated Test: Process multiple transactions and verify block commitment")
    @Order(3)
    void repeatedTest() throws LedgerException {
        Account master = ledger.getUncommittedBlock().getAccount("master");
        Account alice = ledger.createAccount("alice");
        Account bob = ledger.createAccount("bob");

        for (int i = 1; i <= 10; i++) {
            Transaction tx = new Transaction(String.valueOf(i), 50 * i, 15, "repeated test tx", master, i % 2 == 0 ? alice : bob);
            String txId = ledger.processTransaction(tx);
            assertEquals(String.valueOf(i), txId);
        }

        assertEquals(1, ledger.getNumberOfBlocks());

        Block block = ledger.getBlock(1);
        assertEquals(10, block.getTransactionList().size());

        Map<String, Integer> balances = ledger.getAccountBalances();
        assertNotNull(balances);
        assertTrue(balances.get("alice") > 0);
        assertTrue(balances.get("bob") > 0);

        System.out.println("Alice: " + balances.get("alice") + ", Bob: " + balances.get("bob"));
    }


    @Test
    @DisplayName("Test: Lifecycle Demonstration - BeforeAll, BeforeEach, AfterEach, AfterAll")
    @Order(4)
    void lifeCycleTest() throws LedgerException {
        Account master = ledger.getUncommittedBlock().getAccount("master");
        Account testUser = ledger.createAccount("lifecycleUser");

        assertNotNull(master, "Master account should be initialized in BeforeAll/BeforeEach");
        assertNotNull(testUser, "Test user should be created");

        for (int i = 1; i <= 10; i++) {
            Transaction tx = new Transaction(String.valueOf(i), 100, 15, "lifecycle test", master, testUser);
            ledger.processTransaction(tx);
        }

        assertEquals(1, ledger.getNumberOfBlocks());
        assertEquals(1000, ledger.getAccountBalance("lifecycleUser"));

        System.out.println("Ledger was initialized by BeforeAll, reset by BeforeEach. AfterEach and AfterAll will log completion.");
    }

    @Test
    @EnabledOnOs({OS.MAC, OS.LINUX, OS.WINDOWS})
    @DisplayName("Conditional Test: Run on specific OS")
    @Order(5)
    void conditionalTest() throws LedgerException {
        assumeTrue(ledger != null, "Ledger must be initialized");
        Account account = ledger.createAccount("conditionalAccount");
        assertNotNull(account);
    }

    @Test
    @DisplayName("Test: Tagged Test")
    @Order(6)
    @Tag("transaction")
    @Tag("ledger")
    void taggedTest() throws LedgerException {
        Account master = ledger.getUncommittedBlock().getAccount("master");
        Account user1 = ledger.createAccount("user1");
        Account user2 = ledger.createAccount("user2");

        assertThrows(LedgerException.class, () -> {
            ledger.createAccount("user2");
        });

        for (int i = 1; i <= 10; i++) {
            Transaction tx = new Transaction(String.valueOf(i), 50, 10, "tagged test", master, i % 2 == 0 ? user1 : user2);
            ledger.processTransaction(tx);
        }

        Block block1 = ledger.getBlock(1);
        assertNotNull(block1.getPreviousHash(), "Block should have previous hash");
        assertEquals(10, block1.getTransactionList().size(), "Block should have 10 transactions");

        Transaction tx5 = ledger.getTransaction("5");
        assertEquals("5", tx5.getTransactionId());
        assertEquals(50, tx5.getAmount());
        assertEquals(10, tx5.getFee());
        assertEquals("tagged test", tx5.getNote());

        Map<String, Integer> balances = ledger.getAccountBalances();
        assertNotNull(balances);
        assertEquals(3, block1.getAccountBalanceMap().size());

        System.out.println("Tagged Test: Tested block methods, transaction getters, and account balances");
    }

    @Nested
    @DisplayName("Nested Tests: Ledger Getters and Setters")
    @Order(7)
    class NestedTestClass {

        @Test
        @DisplayName("Test Ledger name getter and setter")
        void nestedTestName() {
            ledger.setName("NewName");
            assertEquals("NewName", ledger.getName());
        }

        @Test
        @DisplayName("Test Ledger description getter and setter")
        void nestedTestDescription() {
            ledger.setDescription("New Description");
            assertEquals("New Description", ledger.getDescription());
        }

        @Test
        @DisplayName("Test Ledger seed getter and setter")
        void nestedTestSeed() {
            ledger.setSeed("new-seed");
            assertEquals("new-seed", ledger.getSeed());
        }
    }
    
    @Test
    @DisplayName("Basic Assertions Test")
    @Order(8)
    void basicAssertionsTest() throws LedgerException { 
        
        Block block = ledger.getUncommittedBlock();
        block.setBlockNumber(5);
        // 1. assertEquals
        assertEquals(5, block.getBlockNumber(), "Block number should be 5");

        // 2. assertNotNull with Merkle Tree
        MerkleTrees merkleTree = new MerkleTrees(new java.util.ArrayList<>());
        assertNotNull(merkleTree, "Merkle tree should not be null");

        // 3. assertTrue
        block.setPreviousHash("abc123");
        assertTrue(block.getPreviousHash().equals("abc123"), "Previous hash should be 'abc123'");

        Account testAccount = ledger.createAccount("testAccount");
        testAccount.setAddress("settedAddress");
        // 4. assertFalse
        assertFalse(testAccount.getAddress().equals("wrongAddress"), "Account address should not be 'wrongAddress'");

        // 5. assertNull
        Account nullAccount = null;
        assertNull(nullAccount, "Null account should be null");

    }

    @Test
    @DisplayName("Advanced Assertions Test: Transaction Setters")
    @Order(9)
    void advancedAssertionsTest() throws LedgerException {

        // Setup: Create test accounts and transaction
        Account payer = ledger.createAccount("payer");
        Account receiver = ledger.createAccount("receiver");
        Transaction tx = new Transaction("test-id", 100, 10, "test note", payer, receiver);

        // ADVANCED ASSERTION 1: assertAll - Test multiple setter operations together
        assertAll("Testing multiple Transaction setters",
            () -> assertDoesNotThrow(() -> tx.setTransactionId("new-id"), "setTransactionId should not throw"),
            () -> assertEquals("new-id", tx.getTransactionId(), "Transaction ID should be updated"),
            () -> assertDoesNotThrow(() -> tx.setAmount(200), "setAmount should not throw"),
            () -> assertEquals(200, tx.getAmount(), "Amount should be updated to 200"),
            () -> assertDoesNotThrow(() -> tx.setFee(15), "setFee should not throw"),
            () -> assertEquals(15, tx.getFee(), "Fee should be updated to 15")
        );

        // ADVANCED ASSERTION 2: assertAll - Test string-based setters
        assertAll("Testing string setters",
            () -> assertDoesNotThrow(() -> tx.setNote("updated note"), "setNote should not throw"),
            () -> assertEquals("updated note", tx.getNote(), "Note should be updated"),
            () -> assertNotNull(tx.getNote(), "Note should not be null after setting")
        );

        // ADVANCED ASSERTION 3: assertDoesNotThrow - Test setter operations individually
        Account newPayer = ledger.createAccount("new-payer");
        assertDoesNotThrow(() -> tx.setPayer(newPayer), "setPayer should not throw");
        assertEquals(newPayer, tx.getPayer(), "Payer should be updated to new account");
        assertNotNull(tx.getPayer(), "Payer should not be null");

        // ADVANCED ASSERTION 4: assertThrows - Test that duplicate account creation throws exception
        assertThrows(LedgerException.class, () -> {
            ledger.createAccount("new-payer");
        }, "Creating duplicate account should throw LedgerException");

        Account newReceiver = ledger.createAccount("new-receiver");
        assertDoesNotThrow(() -> tx.setReceiver(newReceiver), "setReceiver should not throw");
        assertEquals(newReceiver, tx.getReceiver(), "Receiver should be updated to new account");
        assertNotNull(tx.getReceiver(), "Receiver should not be null");

        // ADVANCED ASSERTION 5: assertAll 
        Transaction tx2 = new Transaction("id-1", 50, 20, "note-1", payer, receiver);
        assertAll("Comprehensive setter test on new transaction",
            () -> {
                tx2.setTransactionId("id-updated");
                assertEquals("id-updated", tx2.getTransactionId());
            },
            () -> {
                tx2.setAmount(500);
                assertEquals(500, tx2.getAmount());
            },
            () -> {
                tx2.setFee(25);
                assertEquals(25, tx2.getFee());
            },
            () -> {
                tx2.setNote("updated note for transaction 2");
                assertEquals("updated note for transaction 2", tx2.getNote());
            },
            () -> {
                Account account1 = ledger.createAccount("account1");
                tx2.setPayer(account1);
                assertEquals(account1, tx2.getPayer());
            },
            () -> {
                Account account2 = ledger.createAccount("account2");
                tx2.setReceiver(account2);
                assertEquals(account2, tx2.getReceiver());
            }
        );

        // ADVANCED ASSERTION 6: assertTimeout
        assertTimeout(Duration.ofSeconds(5), () -> {
            Account timeoutTestAccount = ledger.createAccount("timeout-test");
            assertNotNull(timeoutTestAccount, "Account should be created within timeout");
            Transaction timeoutTestTx = new Transaction("timeout-id", 100, 10, "timeout test", payer, receiver);
            assertNotNull(timeoutTestTx, "Transaction should be created within timeout");
        }, "Transaction operations should complete within 5 seconds");

    }
    @Test
    @Order(10)
    @DisplayName("Test: Mock Behavior - Using Mockito when/then and doReturn")
    void mockBehaviorTest() throws LedgerException {

        // BEHAVIOR 1: Mock Account with when/then to configure return value
        Account mockAccount = mock(Account.class);
        when(mockAccount.getAddress()).thenReturn("mocked-address");
        when(mockAccount.getBalance()).thenReturn(5000);

        assertEquals("mocked-address", mockAccount.getAddress(), "Mock account should return configured address");
        assertEquals(5000, mockAccount.getBalance(), "Mock account should return configured balance");

        // BEHAVIOR 2: Mock Transaction with multiple configured behaviors
        Transaction mockTx = mock(Transaction.class);
        when(mockTx.getTransactionId()).thenReturn("mock-tx-id");
        when(mockTx.getAmount()).thenReturn(250);
        when(mockTx.getFee()).thenReturn(12);
        when(mockTx.getNote()).thenReturn("mocked transaction");

        assertEquals("mock-tx-id", mockTx.getTransactionId(), "Mock transaction should return configured ID");
        assertEquals(250, mockTx.getAmount(), "Mock transaction should return configured amount");
        assertEquals(12, mockTx.getFee(), "Mock transaction should return configured fee");
        assertEquals("mocked transaction", mockTx.getNote(), "Mock transaction should return configured note");

        // BEHAVIOR 3: Mock Block with doReturn behavior
        Block mockBlock = mock(Block.class);
        doReturn("mock-hash-value").when(mockBlock).getPreviousHash();
        doReturn(5).when(mockBlock).getBlockNumber();

        assertEquals("mock-hash-value", mockBlock.getPreviousHash(), "Mock block should return configured previous hash");
        assertEquals(5, mockBlock.getBlockNumber(), "Mock block should return configured block number");
    }

    @Test
    @DisplayName("Test: Assumptions (assumeTrue, assumeFalse, assumingThat)")
    @Order(11)
    void assumptionsTest() throws LedgerException {

        // ASSUMPTION 1: assumeTrue - Test only runs if ledger is not null
        assumeTrue(ledger != null, "Ledger must be initialized");

        Account testAccount = ledger.createAccount("assumption-test-account");
        assertNotNull(testAccount, "Account should be created");

        // ASSUMPTION 2: assumeFalse - Test only runs if certain condition is false
        boolean isTestMode = false;
        assumeFalse(isTestMode, "This test should not run in test mode");

        // This code only executes if isTestMode is false
        assertEquals(0, testAccount.getBalance(), "New account should have zero balance");

        // ASSUMPTION 3: assumingThat - Conditional execution of assertions
        String environment = "production";
        assumingThat(
            environment.equals("production"),
            () -> {
                // These assertions only run if environment is "production"
                assertNotNull(ledger.getUncommittedBlock(), "Uncommitted block should exist in production");
                assertTrue(ledger.getNumberOfBlocks() >= 0, "Block count should be non-negative in production");
            }
        );

    }

    @Test
    @Order(12)
    @DisplayName("Test: Mock Verification - Using verify, times, never, atLeast")
    void mockVerificationTest() {

        // VERIFICATION 1: Verify method was called a specific number of times
        Account mockAccount = mock(Account.class);

        mockAccount.setAddress("addr1");
        mockAccount.setAddress("addr2");
        mockAccount.setAddress("addr3");
        mockAccount.getAddress();

        verify(mockAccount, times(3)).setAddress(anyString());
        verify(mockAccount, times(1)).getAddress();
        System.out.println("VERIFICATION 1: Method call count verified (times)");

        // VERIFICATION 2: Verify method was never called
        Transaction mockTx = mock(Transaction.class);
        mockTx.getAmount();

        verify(mockTx, never()).setFee(anyInt());
        verify(mockTx, times(1)).getAmount();
        System.out.println("VERIFICATION 2: Verified method was never called (never)");

        // VERIFICATION 3: Verify method was called at least a certain number of times
        Block mockBlock = mock(Block.class);

        mockBlock.getBlockNumber();
        mockBlock.getBlockNumber();
        mockBlock.getPreviousHash();
        mockBlock.getBlockNumber();

        verify(mockBlock, atLeast(3)).getBlockNumber();
        verify(mockBlock, atLeastOnce()).getPreviousHash();
        System.out.println("VERIFICATION 3: Verified method call frequency (atLeast, atLeastOnce)");
    }
    @Test
    @Order(13)
    @DisplayName("Test: Mock Argument Matchers - Using any(), eq(), contains(), startsWith()")
    void mockArgumentMatchersTest() {

        // ARGUMENT MATCHER 1: Using any() to match any value
        Account mockAccount = mock(Account.class);
        when(mockAccount.getBalance()).thenReturn(1000);

        mockAccount.setAddress("test-address");
        verify(mockAccount).setAddress(any());
        verify(mockAccount).setAddress(any(String.class));
        System.out.println("ARGUMENT MATCHER 1: any() matcher verified");

        // ARGUMENT MATCHER 2: Using eq() to match exact values
        Transaction mockTx = mock(Transaction.class);
        when(mockTx.getFee()).thenReturn(15);

        mockTx.setAmount(500);
        mockTx.setFee(15);

        verify(mockTx).setAmount(eq(500));
        verify(mockTx).setFee(eq(15));
        System.out.println("ARGUMENT MATCHER 2: eq() matcher verified for exact value matching");

        // ARGUMENT MATCHER 3: Using contains() and startsWith() for string matching
        Block mockBlock = mock(Block.class);

        mockBlock.setPreviousHash("abc123def456");

        verify(mockBlock).setPreviousHash(contains("123"));
        verify(mockBlock).setPreviousHash(startsWith("abc"));
        System.out.println("ARGUMENT MATCHER 3: contains() and startsWith() matchers verified");

        // ARGUMENT MATCHER 4: Using anyInt(), anyString() for type-specific matchers
        Transaction mockTx2 = mock(Transaction.class);

        mockTx2.setAmount(750);
        mockTx2.setNote("payment for services");

        verify(mockTx2).setAmount(anyInt());
        verify(mockTx2).setNote(anyString());
        System.out.println("ARGUMENT MATCHER 4: anyInt() and anyString() type-specific matchers verified");
    }

    @Test
    @Order(14)
    @DisplayName("Test: Method Ordering with @Order annotation")
    void methodOrderTest() throws LedgerException {

        Account account = ledger.createAccount("order-test");
        assertNotNull(account);
        assertTrue(testCounter > 0, "At least one test should have run before this");

    }

    @Test
    @Order(15)
    @DisplayName("End-to-End Ledger Test")
    void endToEndLedgerTest() throws LedgerException {
        // TODO: Produce/Print Identical Results to Command Line DriverTest
        // TODO: Complete end-to-end test to demonstrate Arrange-ACT-Assert pattern
        //  - Arrange: Create a new ledger instance with proper initialization and reset
        //  - Act: Execute all the blockchain operations (account creation, transactions, validations, etc.)
        //  - Assert: Use JUnit assertions throughout to verify expected behavior at each step

        // Create ledger instance with test parameters
        ledger.reset();
        ledger.setName("test");
        ledger.setDescription("test ledger 2025");
        ledger.setSeed("chapman");
        assertEquals("test", ledger.getName());
        assertEquals("test ledger 2025", ledger.getDescription());
        assertEquals("chapman", ledger.getSeed());
        System.out.println("Creating Ledger: test test ledger 2025 chapman");

        // Create accounts for testing
        Account mary = ledger.createAccount("mary");
        Account bob = ledger.createAccount("bob");
        Account bill = ledger.createAccount("bill");
        Account frank = ledger.createAccount("frank");
        Account jane = ledger.createAccount("jane");

        // Assert: Verify all accounts were created successfully
        assertNotNull(mary);
        assertNotNull(bill);
        assertNotNull(bob);
        assertNotNull(frank);
        assertNotNull(jane);

        System.out.println("Creating Account: mary");
        System.out.println("Creating Account: bob");
        System.out.println("Creating Account: bill");
        System.out.println("Creating Account: frank");
        System.out.println("Creating Account: jane");

        // Assert: Try to get balance for mary (should fail - not committed to block yet)
        assertThrows(LedgerException.class, () -> {
            ledger.getAccountBalance("mary");
        });

        // Get master account from uncommitted block
        Block block = ledger.getUncommittedBlock();
        Account master = block.getAccount("master");

        // Create transactions 1-5 to fund accounts
        Transaction t1 = new Transaction("1", 1000, 10, "fund account", master, mary);
        Transaction t2 = new Transaction("2", 1000, 10, "fund account", master, bob);
        Transaction t3 = new Transaction("3", 1000, 10, "fund account", master, bill);
        Transaction t4 = new Transaction("4", 1000, 10, "fund account", master, frank);
        Transaction t5 = new Transaction("5", 1000, 10, "fund account", master, mary);

        // Process transactions 1-5 (block not yet committed)
        String t1Id = ledger.processTransaction(t1);
        String t2Id = ledger.processTransaction(t2);
        String t3Id = ledger.processTransaction(t3);
        String t4Id = ledger.processTransaction(t4);
        String t5Id = ledger.processTransaction(t5);

        // Assert: Verify transaction IDs match
        assertEquals("1",t1Id);
        assertEquals("2",t2Id);
        assertEquals("3",t3Id);
        assertEquals("4",t4Id);
        assertEquals("5",t5Id);

        System.out.println("Processing Transaction: 1 1000 10 fund account master mary");
        System.out.println("Processing Transaction: 2 1000 10 fund account master bob");
        System.out.println("Processing Transaction: 3 1000 10 fund account master bill");
        System.out.println("Processing Transaction: 4 1000 10 fund account master frank");
        System.out.println("Processing Transaction: 5 1000 10 fund account master mary");

        // Assert: Verify balances not yet committed (should throw exception)
        assertThrows(LedgerException.class, () -> {
            ledger.getAccountBalance("mary");
        });

        // Assert: Verify no account balances available yet (block not committed)
        assertNull(ledger.getAccountBalances());

        // Create transactions 6-10 (these will trigger block 1 commitment when t10 is processed)
        Transaction t6 = new Transaction("6", 1000, 10, "fund account", master, bob);
        Transaction t7 = new Transaction("7", 1000, 10, "fund account", master, bill);
        Transaction t8 = new Transaction("8", 1000, 10, "fund account", master, frank);
        Transaction t9 = new Transaction("9", 1000, 10, "fund account", master, mary);
        Transaction t10 = new Transaction("10", 1000, 10, "fund account", master, bob);

        // Process transactions 6-10 (t10 triggers block 1 commitment)
        String t6Id = ledger.processTransaction(t6);
        String t7Id = ledger.processTransaction(t7);
        String t8Id = ledger.processTransaction(t8);
        String t9Id = ledger.processTransaction(t9);
        String t10Id = ledger.processTransaction(t10);

        assertEquals("6",t6Id);
        assertEquals("7",t7Id);
        assertEquals("8",t8Id);
        assertEquals("9",t9Id);
        assertEquals("10",t10Id);

        System.out.println("Processing Transaction: 6 1000 10 fund account master bob");
        System.out.println("Processing Transaction: 7 1000 10 fund account master bill");
        System.out.println("Processing Transaction: 8 1000 10 fund account master frank");
        System.out.println("Processing Transaction: 9 1000 10 fund account master mary");
        System.out.println("Processing Transaction: 10 1000 10 fund account master bob");

        // Assert: Verify account balances after block 1 commits
        // Block 1 now contains all 10 transactions and is committed
        assertEquals(3000, mary.getBalance());
        assertEquals(3000, bob.getBalance());
        assertEquals(2000, bill.getBalance());
        assertEquals(2000, frank.getBalance());
        assertEquals(0, jane.getBalance());

        System.out.println("Getting Balance for: mary");
        System.out.println("Account Balance for: mary is 3000");

        System.out.println("Getting Balance for: bob");
        System.out.println("Account Balance for: bob is 3000");

        System.out.println("Getting Balance for: bill");
        System.out.println("Account Balance for: bill is 2000");    

        System.out.println("Getting Balance for: frank");
        System.out.println("Account Balance for: frank is 2000");   

        System.out.println("Getting Balance for: jane");
        System.out.println("Account Balance for: jane is 0");

        // Assert: Retrieve and verify block 1 and transaction 10 details
        Block block1 = ledger.getBlock(1);
        assertNotNull(block1);
        assertEquals(1, block1.getBlockNumber());
        System.out.println("Getting Block: 1");

        Transaction getTransaction10 = ledger.getTransaction("10");
        assertNotNull(getTransaction10);
        assertEquals("10", getTransaction10.getTransactionId());
        System.out.println("Getting Transaction: 10");

        // Act: Get fresh account references from uncommitted block for next transactions
        Block block2 = ledger.getUncommittedBlock();
        Account frank2 = block2.getAccount("frank");
        Account jane2 = block2.getAccount("jane");
        Account bob2 = block2.getAccount("bob");
        Account mary2 = block2.getAccount("mary");

        // Act: Process transaction 11 (transfer 200 from frank to jane)
        Transaction t11 = new Transaction("11", 200, 10, "september rent", frank2, jane2);
        String t11Id = ledger.processTransaction(t11);
        assertEquals("11", t11Id);
        System.out.println("Processing Transaction: 11 200 10 september rent frank jane");

        // Assert: Verify balances after t11 
        assertEquals(2000, ledger.getAccountBalance("frank"));
        assertEquals(0, ledger.getAccountBalance("jane"));
        System.out.println("Getting Balance for: frank");
        System.out.println("Account Balance for: frank is 2000");
        System.out.println("Getting Balance for: jane");
        System.out.println("Account Balance for: jane is 0");

        // Act: Create transactions 12-20
        Transaction t12 = new Transaction("12", 20, 10, "uber", bob2, mary2);
        Transaction t13 = new Transaction("13", 20, 10, "uber", bob2, mary2);
        Transaction t14 = new Transaction("14", 20, 10, "uber", bob2, mary2);
        Transaction t15 = new Transaction("15", 20, 10, "uber", bob2, mary2);
        Transaction t16 = new Transaction("16", 20, 10, "uber", bob2, mary2);
        Transaction t17 = new Transaction("17", 20, 10, "uber", bob2, mary2);
        Transaction t18 = new Transaction("18", 20, 10, "uber", bob2, mary2);
        Transaction t19 = new Transaction("19", 20, 10, "uber", bob2, mary2);
        Transaction t20 = new Transaction("20", 20, 10, "uber", bob2, mary2);

        // Process transactions 12-20 
        String t12Id = ledger.processTransaction(t12);
        String t13Id = ledger.processTransaction(t13);
        String t14Id = ledger.processTransaction(t14);
        String t15Id = ledger.processTransaction(t15);
        String t16Id = ledger.processTransaction(t16);
        String t17Id = ledger.processTransaction(t17);
        String t18Id = ledger.processTransaction(t18);
        String t19Id = ledger.processTransaction(t19);
        String t20Id = ledger.processTransaction(t20);

        // Assert: Verify transaction IDs for t12-t20
        assertEquals("12", t12Id);
        assertEquals("13", t13Id);
        assertEquals("14", t14Id);
        assertEquals("15", t15Id);
        assertEquals("16", t16Id);
        assertEquals("17", t17Id);
        assertEquals("18", t18Id);
        assertEquals("19", t19Id);
        assertEquals("20", t20Id);

        System.out.println("Processing Transaction: 12 20 10 uber bob mary");
        System.out.println("Processing Transaction: 13 20 10 uber bob mary");
        System.out.println("Processing Transaction: 14 20 10 uber bob mary");
        System.out.println("Processing Transaction: 15 20 10 uber bob mary");
        System.out.println("Processing Transaction: 16 20 10 uber bob mary");
        System.out.println("Processing Transaction: 17 20 10 uber bob mary");
        System.out.println("Processing Transaction: 18 20 10 uber bob mary");
        System.out.println("Processing Transaction: 19 20 10 uber bob mary");
        System.out.println("Processing Transaction: 20 20 10 uber bob mary");

        // Assert: Verify account balances after block 2 commits
        assertEquals(200, ledger.getAccountBalance("jane"));
        assertEquals(1790, ledger.getAccountBalance("frank"));
        assertEquals(3180, ledger.getAccountBalance("mary"));
        assertEquals(2730, ledger.getAccountBalance("bob"));

        System.out.println("Getting Balance for: jane");
        System.out.println("Account Balance for: jane is 200");
        System.out.println("Getting Balance for: frank");
        System.out.println("Account Balance for: frank is 1790");
        System.out.println("Getting Balance for: mary");
        System.out.println("Account Balance for: mary is 3180");
        System.out.println("Getting Balance for: bob");
        System.out.println("Account Balance for: bob is 2730");

        // Arrange/Act: Get all account balances
        Map<String, Integer> balances = ledger.getAccountBalances();
        // Assert: Verify all account balances
        assertNotNull(balances);
        assertEquals(200, balances.get("jane"));
        assertEquals(1790, balances.get("frank"));
        assertEquals(3180, balances.get("mary"));
        assertEquals(2730, balances.get("bob"));
        System.out.println("Getting Account Balances");
        System.out.println("Account Balances: " + balances.toString());

        // Assert: Test error cases
        // Transaction exceeds account balance
        assertThrows(LedgerException.class, () -> {
            Transaction t21 = new Transaction("21", 5000, 10, "food", bob, mary);
            ledger.processTransaction(t21);
        });
        System.out.println("Processing Transaction: 21 5000 10 food bob mary");
        System.out.println("Error: Payer Does Not Have Required Funds");

        // Transaction fee is too low
        assertThrows(LedgerException.class, () -> {
            Transaction t22 = new Transaction("22", 20, 5, "food", bob, mary);
            ledger.processTransaction(t22);
        });
        System.out.println("Processing Transaction: 22 20 5 food bob mary");
        System.out.println("Error: Transaction Fee Must Be Greater Than 10");

        // Non-existent account
        assertThrows(LedgerException.class, () -> {
            ledger.getAccountBalance("sergey");
        });
        System.out.println("Getting Balance for: sergey");
        System.out.println("Error: Account Does Not Exist");

        // Assert: Validate
        assertDoesNotThrow(() -> {
            ledger.validate();
        });
        System.out.println("Validate: Valid");
    }

    @Test
    @Order(16)
    @DisplayName("Test: MerkleTrees getSHA2HexValue coverage")
    void merkleSHA2Test() {
        // Test the MerkleTrees getSHA2HexValue method for edge cases
        MerkleTrees merkleTree = new MerkleTrees(new java.util.ArrayList<>());

        // Test 5: Null input - should trigger exception handling and return empty string
        String hash5 = merkleTree.getSHA2HexValue(null);
        assertEquals("", hash5, "getSHA2HexValue(null) should return empty string due to exception handling");

    }

    @Test
    @Order(17)
    @DisplayName("Test: LedgerException setAction/setReason and getAction/getReason")
    void ledgerExceptionTest() {
        LedgerException exception = new LedgerException("AccountError", "Account not found");

        // Test getAction and getReason from constructor
        assertEquals("AccountError", exception.getAction(), "Action should be initialized correctly");
        assertEquals("Account not found", exception.getReason(), "Reason should be initialized correctly");

        // Test setAction and getAction
        exception.setAction("TransactionProcessing");
        assertEquals("TransactionProcessing", exception.getAction(), "Action should be set and retrieved correctly");

        // Test setReason and getReason
        exception.setReason("Insufficient funds");
        assertEquals("Insufficient funds", exception.getReason(), "Reason should be set and retrieved correctly");
    }

    @Test
    @Order(18)
    @DisplayName("Test: Process Transaction Validation")
    void processTransactionTest() throws LedgerException {
        // Setup
        Account payer = ledger.createAccount("payer");
        Account receiver = ledger.createAccount("receiver");
        Account master = ledger.getUncommittedBlock().getAccount("master");

        // Fund the payer
        Transaction fundTx = new Transaction("fund-tx", 5000, 15, "initial fund", master, payer);
        ledger.processTransaction(fundTx);

        // Test 1: Valid transaction should succeed
        Transaction validTx = new Transaction("valid-tx", 100, 15, "valid transaction", payer, receiver);
        assertDoesNotThrow(() -> ledger.processTransaction(validTx));

        // Test 2: Fee too low should throw exception
        Transaction lowFeeTx = new Transaction("low-fee-tx", 100, 5, "fee too low", payer, receiver);
        assertThrows(LedgerException.class, () -> ledger.processTransaction(lowFeeTx));

        // Test 3: Amount out of range should throw exception
        Transaction negativeTx = new Transaction("negative-tx", -100, 15, "negative amount", payer, receiver);
        assertThrows(LedgerException.class, () -> ledger.processTransaction(negativeTx));

        // Test 4: Note too long should throw exception
        String longNote = "x".repeat(1025);
        Transaction longNoteTx = new Transaction("long-note-tx", 100, 15, longNote, payer, receiver);
        assertThrows(LedgerException.class, () -> ledger.processTransaction(longNoteTx));
    }

    @Test
    @Order(19)
    @DisplayName("Test: getBlock(Integer)")
    void getBlockTest() throws LedgerException {
        // Setup: Create accounts and process transactions to create a committed block
        Account master = ledger.getUncommittedBlock().getAccount("master");
        Account account1 = ledger.createAccount("account1");

        // Process 10 transactions to commit block 1
        for (int i = 1; i <= 10; i++) {
            Transaction tx = new Transaction(String.valueOf(i), 100, 15, "test", master, account1);
            ledger.processTransaction(tx);
        }

        // TEST 1: Successfully retrieve an existing block (happy path)
        Block block1 = ledger.getBlock(1);
        assertNotNull(block1, "Block 1 should exist and be returned");
        assertEquals(1, block1.getBlockNumber(), "Block number should be 1");
        assertEquals(10, block1.getTransactionList().size(), "Block 1 should contain 10 transactions");

        // TEST 2: Attempt to retrieve a non-existent block (exception path)
        LedgerException exception = assertThrows(LedgerException.class, () -> {
            ledger.getBlock(999);
        }, "getBlock(999) should throw LedgerException for non-existent block");

        // Verify exception details
        assertEquals("Get Block", exception.getAction(), "Exception action should be 'Get Block'");
        assertEquals("Block Does Not Exist", exception.getReason(), "Exception reason should be 'Block Does Not Exist'");

        // TEST 3: Verify block 0 doesn't exist either
        LedgerException exceptionZero = assertThrows(LedgerException.class, () -> {
            ledger.getBlock(0);
        }, "getBlock(0) should throw LedgerException");
        assertEquals("Block Does Not Exist", exceptionZero.getReason(), "Non-existent block 0 should throw appropriate exception");

        // TEST 4: Verify block with negative number doesn't exist
        LedgerException exceptionNegative = assertThrows(LedgerException.class, () -> {
            ledger.getBlock(-1);
        }, "getBlock(-1) should throw LedgerException");
        assertEquals("Block Does Not Exist", exceptionNegative.getReason(), "Non-existent negative block should throw appropriate exception");
    }

    @Test
    @Order(20)
    @DisplayName("Test: getInstance(String, String, String)")
    void getInstanceTest() throws Exception {
        // TEST 1: Reset singleton to null using reflection to test the null case
        Field ledgerField = Ledger.class.getDeclaredField("ledger");
        ledgerField.setAccessible(true);
        ledgerField.set(null, null);

        // Verify ledger is now null
        assertNull(ledgerField.get(null), "Ledger field should be null before first getInstance call");

        // TEST 1a: Call getInstance when ledger is null (creates new instance)
        Ledger instance1 = Ledger.getInstance("TestLedger1", "First Description", "seed-1");
        assertNotNull(instance1, "getInstance should create and return a new Ledger instance when ledger is null");
        assertEquals("TestLedger1", instance1.getName(), "Ledger name should be 'TestLedger1'");
        assertEquals("First Description", instance1.getDescription(), "Ledger description should match");
        assertEquals("seed-1", instance1.getSeed(), "Ledger seed should match");

        // TEST 2: Call getInstance again with different parameters (returns existing instance)
        Ledger instance2 = Ledger.getInstance("TestLedger2", "Second Description", "seed-2");
        assertNotNull(instance2, "getInstance should return existing Ledger instance when ledger is not null");

        // Verify it's the SAME instance (singleton pattern)
        assertSame(instance1, instance2, "getInstance should return the same instance on subsequent calls");

        // Verify parameters from first call are preserved (not overwritten)
        assertEquals("TestLedger1", instance2.getName(), "Singleton name should remain from first call, not be overwritten");
        assertEquals("First Description", instance2.getDescription(), "Singleton description should remain from first call");
        assertEquals("seed-1", instance2.getSeed(), "Singleton seed should remain from first call");

        // TEST 3: Call getInstance a third time with yet another parameter set
        Ledger instance3 = Ledger.getInstance("TestLedger3", "Third Description", "seed-3");
        assertSame(instance1, instance3, "Third call should still return the same singleton instance");
        assertEquals("TestLedger1", instance3.getName(), "Parameters should still be from the first initialization");

        // TEST 4: Verify all three references point to the same object in memory
        assertTrue(instance1 == instance2 && instance2 == instance3, "All instances should be references to the same object");

        // Restore the global ledger instance for other tests
        Ledger.getInstance("TestLedger", "Test Blockchain", "test-seed");
    }

    @Test
    @Order(21)
    @DisplayName("Test: getTransaction(String)")
    void getTransactionTest() throws LedgerException {
        // Setup: Create accounts for transactions
        Account master = ledger.getUncommittedBlock().getAccount("master");
        Account alice = ledger.createAccount("alice");
        Account bob = ledger.createAccount("bob");
        Account charlie = ledger.createAccount("charlie");

        // Create 10 transactions (will be committed to block 1)
        Transaction t1 = new Transaction("tx-001", 100, 15, "committed block tx", master, alice);
        Transaction t2 = new Transaction("tx-002", 200, 15, "committed block tx", master, bob);
        Transaction t3 = new Transaction("tx-003", 300, 15, "committed block tx", master, charlie);
        Transaction t4 = new Transaction("tx-004", 400, 15, "committed block tx", master, alice);
        Transaction t5 = new Transaction("tx-005", 500, 15, "committed block tx", master, bob);
        Transaction t6 = new Transaction("tx-006", 600, 15, "committed block tx", master, charlie);
        Transaction t7 = new Transaction("tx-007", 700, 15, "committed block tx", master, alice);
        Transaction t8 = new Transaction("tx-008", 800, 15, "committed block tx", master, bob);
        Transaction t9 = new Transaction("tx-009", 900, 15, "committed block tx", master, charlie);
        Transaction t10 = new Transaction("tx-010", 1000, 15, "committed block tx", master, alice);

        // Process all 10 transactions to commit block 1
        ledger.processTransaction(t1);
        ledger.processTransaction(t2);
        ledger.processTransaction(t3);
        ledger.processTransaction(t4);
        ledger.processTransaction(t5);
        ledger.processTransaction(t6);
        ledger.processTransaction(t7);
        ledger.processTransaction(t8);
        ledger.processTransaction(t9);
        ledger.processTransaction(t10);

        // Verify block 1 is committed
        assertEquals(1, ledger.getNumberOfBlocks(), "Block 1 should be committed");

        // TEST 1: Find transaction in committed block (path through committed blocks)
        Transaction foundTx1 = ledger.getTransaction("tx-001");
        assertNotNull(foundTx1, "Should find transaction tx-001 in committed block");
        assertEquals("tx-001", foundTx1.getTransactionId(), "Transaction ID should match");
        assertEquals(100, foundTx1.getAmount(), "Transaction amount should match");

        // TEST 2: Find different transaction in committed block
        Transaction foundTx5 = ledger.getTransaction("tx-005");
        assertNotNull(foundTx5, "Should find transaction tx-005 in committed block");
        assertEquals("tx-005", foundTx5.getTransactionId(), "Transaction ID should match");
        assertEquals(500, foundTx5.getAmount(), "Transaction amount should match");

        // TEST 3: Find last transaction in committed block
        Transaction foundTx10 = ledger.getTransaction("tx-010");
        assertNotNull(foundTx10, "Should find transaction tx-010 in committed block");
        assertEquals("tx-010", foundTx10.getTransactionId(), "Transaction ID should match");

        // Create transactions in uncommitted block (block 2)
        Transaction tx11 = new Transaction("tx-011", 1100, 15, "uncommitted block tx", master, alice);
        Transaction tx12 = new Transaction("tx-012", 1200, 15, "uncommitted block tx", master, bob);

        ledger.processTransaction(tx11);
        ledger.processTransaction(tx12);

        // TEST 4: Find transaction in uncommitted block (path through uncommitted block)
        Transaction foundTx11 = ledger.getTransaction("tx-011");
        assertNotNull(foundTx11, "Should find transaction tx-011 in uncommitted block");
        assertEquals("tx-011", foundTx11.getTransactionId(), "Transaction ID should match");
        assertEquals(1100, foundTx11.getAmount(), "Transaction amount should match");

        // TEST 5: Find another transaction in uncommitted block
        Transaction foundTx12 = ledger.getTransaction("tx-012");
        assertNotNull(foundTx12, "Should find transaction tx-012 in uncommitted block");
        assertEquals("tx-012", foundTx12.getTransactionId(), "Transaction ID should match");
        assertEquals(1200, foundTx12.getAmount(), "Transaction amount should match");

        // TEST 6: Search for non-existent transaction (null return path)
        Transaction notFoundTx = ledger.getTransaction("tx-999");
        assertNull(notFoundTx, "Should return null for non-existent transaction");

        // TEST 7: Search for non-existent transaction with different ID
        Transaction notFoundTx2 = ledger.getTransaction("invalid-tx-id");
        assertNull(notFoundTx2, "Should return null for invalid transaction ID");

        // TEST 8: Search with empty string (edge case)
        Transaction emptyTx = ledger.getTransaction("");
        assertNull(emptyTx, "Should return null for empty transaction ID");
    }

    @Test
    @Order(22)
    @DisplayName("Test: processTransaction(Transaction)")
    void processTransactionSimpleTest() throws LedgerException {
        Account master = ledger.getUncommittedBlock().getAccount("master");
        Account user1 = ledger.createAccount("user1");
        Account user2 = ledger.createAccount("user2");

        // TEST 1: Valid transaction (amount >= 0 AND amount <= Integer.MAX_VALUE)
        Transaction validTx = new Transaction("p1", 100, 15, "valid", master, user1);
        String id = ledger.processTransaction(validTx);
        assertEquals("p1", id, "Valid transaction should return transaction ID");

        // TEST 2: Negative amount (amount < 0 path of OR condition)
        Transaction negativeTx = new Transaction("p2", -50, 15, "bad", master, user1);
        assertThrows(LedgerException.class, () -> ledger.processTransaction(negativeTx));

        // TEST 3: Amount > Integer.MAX_VALUE (using mock to test unreachable branch)
        Transaction mockOverflowTx = mock(Transaction.class);
        when(mockOverflowTx.getAmount()).thenReturn(2147483647); // Integer.MAX_VALUE
        when(mockOverflowTx.getFee()).thenReturn(15);
        when(mockOverflowTx.getNote()).thenReturn("test");
        when(mockOverflowTx.getTransactionId()).thenReturn("p3");
        when(mockOverflowTx.getPayer()).thenReturn(user1);
        when(mockOverflowTx.getReceiver()).thenReturn(master);
        // Both conditions false: amount not < 0 AND amount not > Integer.MAX_VALUE
        assertDoesNotThrow(() -> ledger.processTransaction(mockOverflowTx));

        // TEST 4: Fee too low
        Transaction lowFeeTx = new Transaction("p4", 100, 5, "bad", master, user1);
        assertThrows(LedgerException.class, () -> ledger.processTransaction(lowFeeTx));

        // TEST 5: Note too long
        String longNote = "x".repeat(1025);
        Transaction longNoteTx = new Transaction("p5", 100, 15, longNote, master, user1);
        assertThrows(LedgerException.class, () -> ledger.processTransaction(longNoteTx));

        // TEST 6: Duplicate transaction ID
        Transaction dupTx = new Transaction("p1", 100, 15, "dup", master, user2);
        assertThrows(LedgerException.class, () -> ledger.processTransaction(dupTx));

        // TEST 7: Insufficient funds
        Account poorUser = ledger.createAccount("poor");
        Transaction insufficientTx = new Transaction("p7", 100, 15, "broke", poorUser, master);
        assertThrows(LedgerException.class, () -> ledger.processTransaction(insufficientTx));
    }

    @Test
    @Order(23)
    @DisplayName("Test: validate()")
    void validateTest() throws Exception {
        Account master = ledger.getUncommittedBlock().getAccount("master");
        Account alice = ledger.createAccount("alice");

        // TEST 1: Empty blockMap - should throw "No Block Has Been Committed"
        LedgerException emptyException = assertThrows(LedgerException.class, () -> {
            ledger.validate();
        }, "Validate on empty blockMap should throw exception");
        assertEquals("No Block Has Been Committed", emptyException.getReason(), "Should report no blocks committed");

        // Create 10 transactions to commit block 1
        for (int i = 1; i <= 10; i++) {
            Transaction tx = new Transaction(String.valueOf(i), 100, 15, "test", master, alice);
            ledger.processTransaction(tx);
        }

        // TEST 2: Valid state - should pass without exception
        assertDoesNotThrow(() -> ledger.validate(), "Valid blockchain should pass validation");

        // TEST 3: Create block 2 with 10 transactions
        for (int i = 11; i <= 20; i++) {
            Transaction tx = new Transaction(String.valueOf(i), 100, 15, "test", master, alice);
            ledger.processTransaction(tx);
        }

        // TEST 4: Tamper with block 2's previous hash to trigger hash mismatch
        Block block2 = ledger.getBlock(2);
        block2.setPreviousHash("WRONG_HASH_VALUE");

        LedgerException hashException = assertThrows(LedgerException.class, () -> {
            ledger.validate();
        }, "Tampered hash should fail validation");
        assertEquals("Validate", hashException.getAction(), "Should be validate action");
        assertTrue(hashException.getReason().contains("Hash Is Inconsistent"), "Should report hash inconsistency");

        // Restore correct hash for next test
        Block block1 = ledger.getBlock(1);
        block2.setPreviousHash(block1.getHash());

        // TEST 5: Tamper with transaction count in block 1
        Field transactionListField = Block.class.getDeclaredField("transactionList");
        transactionListField.setAccessible(true);
        java.util.List<Transaction> txList = (java.util.List<Transaction>) transactionListField.get(block1);
        Transaction removedTx = txList.remove(0);

        LedgerException txCountException = assertThrows(LedgerException.class, () -> {
            ledger.validate();
        }, "Wrong transaction count should fail validation");
        assertTrue(txCountException.getReason().contains("Transaction Count Is Not 10 In Block"), "Should report wrong transaction count");

        // Restore transaction count
        txList.add(0, removedTx);

        // TEST 6: Tamper with account balance to trigger balance mismatch
        Block lastBlock = ledger.getBlock(2);
        Field accountMapField = Block.class.getDeclaredField("accountBalanceMap");
        accountMapField.setAccessible(true);
        java.util.Map<String, Account> accountMap = (java.util.Map<String, Account>) accountMapField.get(lastBlock);
        Account aliceRef = accountMap.get("alice");
        aliceRef.setBalance(999999); // Tamper with balance

        LedgerException balanceException = assertThrows(LedgerException.class, () -> {
            ledger.validate();
        }, "Unbalanced ledger should fail validation");
        assertEquals("Balance Does Not Add Up", balanceException.getReason(), "Should report balance mismatch");
    }
}
