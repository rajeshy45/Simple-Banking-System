package banking;

import org.sqlite.SQLiteDataSource;

import java.sql.*;
import java.util.Random;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        String url = "jdbc:sqlite:" + args[1];

        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl(url);

        String createTable = "CREATE TABLE IF NOT EXISTS card (" +
                             "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                             "number TEXT," +
                             "pin TEXT," +
                             "balance INTEGER DEFAULT 0);";

        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(createTable)) {
                if (connection.getAutoCommit()) {
                    connection.setAutoCommit(false);
                }

                preparedStatement.executeUpdate();

                connection.commit();
            } catch (SQLException e) {
                e.printStackTrace();
                connection.rollback();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        while (true) {
            System.out.println("1. Create an account");
            System.out.println("2. Log into account");
            System.out.println("0. Exit");

            int action1 = scanner.nextInt();
            System.out.println();

            if (action1 == 0) {
                System.out.println("Bye!");
                break;
            } else if (action1 == 1) {
                BankAccount bankAccount = new BankAccount();
                System.out.println("Your card has been created");
                System.out.println("Your card number:");
                System.out.println(bankAccount.getCardNumber());
                System.out.println("Your card PIN");
                System.out.println(bankAccount.getPin());

                String insertCard = "INSERT INTO card (number, pin) VALUES (?, ?);";

                try (Connection connection = dataSource.getConnection()) {
                    try (PreparedStatement preparedStatement = connection.prepareStatement(insertCard)) {

                        if (connection.getAutoCommit()) {
                            connection.setAutoCommit(false);
                        }

                        preparedStatement.setString(1, bankAccount.getCardNumber());
                        preparedStatement.setString(2, bankAccount.getPin());
                        preparedStatement.executeUpdate();

                        connection.commit();

                    } catch (SQLException e) {
                        e.printStackTrace();
                        connection.rollback();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }

            } else if (action1 == 2) {
                System.out.println("Enter your card number:");
                String cardNumber = scanner.next();
                System.out.println("Enter your PIN:");
                String pin = scanner.next();
                System.out.println();
                boolean success = false;
                boolean exit = false;

                try (Connection connection = dataSource.getConnection()) {
                    try (Statement statement = connection.createStatement()) {
                        try (ResultSet isSuccess = statement.executeQuery("SELECT * FROM card WHERE number = '"+ cardNumber + "'")) {

                            while (isSuccess.next()) {
                                String haveNum = isSuccess.getString("number");
                                String havePin = isSuccess.getString("pin");

                                if (haveNum.equals(cardNumber)) {
                                    if (havePin.equals(pin)) {
                                        success = true;
                                        break;
                                    }
                                }

                            }

                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                        connection.rollback();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                if (!success) {
                    System.out.println("Wrong card number or PIN!");
                } else {
                    System.out.println("You have successfully logged in!");
                }
                if (success) {
                    while (true) {
                        System.out.println();
                        System.out.println("1. Balance");
                        System.out.println("2. Add income");
                        System.out.println("3. Do transfer");
                        System.out.println("4. Close account");
                        System.out.println("5. Log out");
                        System.out.println("0. Exit");
                        int action2 = scanner.nextInt();
                        System.out.println();
                        if (action2 == 5) {
                            System.out.println("You have successfully logged out!");
                            break;
                        } else if (action2 == 1) {

                            String getBal = "SELECT * FROM card WHERE number = ?";
                            int balance = 0;

                            try (Connection connection = dataSource.getConnection()) {
                                try (PreparedStatement preparedStatement = connection.prepareStatement(getBal)) {
                                    preparedStatement.setString(1, cardNumber);
                                    try (ResultSet bal = preparedStatement.executeQuery()) {
                                        balance = bal.getInt("balance");
                                    }
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                    connection.rollback();
                                }
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }

                            System.out.println("Balance: " + balance);

                        } else if (action2 == 0) {
                            System.out.println("Bye!");
                            exit = true;
                            break;
                        } else if (action2 == 2) {
                            System.out.println("Enter income:");
                            int addAmount = scanner.nextInt();

                            String addBal = "UPDATE card SET balance = balance + ? WHERE number = ?";

                            try (Connection connection = dataSource.getConnection()) {
                                try (PreparedStatement preparedStatement = connection.prepareStatement(addBal)) {
                                    preparedStatement.setString(2, cardNumber);
                                    preparedStatement.setInt(1, addAmount);
                                    preparedStatement.executeUpdate();
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                    connection.rollback();
                                }
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }

                            System.out.println("Income was added!");
                        } else if (action2 == 3) {
                            System.out.println("Transfer");
                            System.out.println("Enter card number:");
                            String transferNumber = scanner.next();
                            boolean found = false;

                            try (Connection connection = dataSource.getConnection()) {
                                try (Statement statement = connection.createStatement()) {
                                    try (ResultSet isFound = statement.executeQuery("SELECT * FROM card WHERE number = '"+ transferNumber + "'")) {

                                        while (isFound.next()) {
                                            String haveNum = isFound.getString("number");

                                            if (haveNum.equals(transferNumber)) {
                                                found = true;
                                                break;
                                            }

                                        }

                                    }
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                    connection.rollback();
                                }
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }

                            if (!found) {
                                if (!testLuhnAlgorithm(transferNumber)) {
                                    System.out.println("Probably you made mistake in the card number. Please try again!");
                                } else {
                                    System.out.println("Such a card does not exist!");
                                }
                            } else {
                                if (cardNumber.equals(transferNumber)) {
                                    System.out.println("You can't transfer money to the same account!");
                                } else {
                                    System.out.println("Enter how much money you want to transfer:");
                                    int transferAmount = scanner.nextInt();

                                    String getBal = "SELECT * FROM card WHERE number = ?";
                                    String transferBal = "UPDATE card SET balance = balance + ? WHERE number = ?";
                                    String updateBal = "UPDATE card SET balance = balance - ? WHERE number = ?";
                                    int balance;

                                    try (Connection connection = dataSource.getConnection()) {
                                        try (PreparedStatement preparedStatement1 = connection.prepareStatement(getBal);
                                             PreparedStatement preparedStatement2 = connection.prepareStatement(transferBal);
                                             PreparedStatement preparedStatement3 = connection.prepareStatement(updateBal)) {
                                            preparedStatement1.setString(1, cardNumber);
                                            preparedStatement2.setInt(1, transferAmount);
                                            preparedStatement2.setString(2, transferNumber);
                                            preparedStatement3.setString(2, cardNumber);
                                            preparedStatement3.setInt(1, transferAmount);
                                            try (ResultSet bal = preparedStatement1.executeQuery()) {
                                                balance = bal.getInt("balance");
                                                if (balance >= transferAmount) {
                                                    preparedStatement2.executeUpdate();
                                                    preparedStatement3.executeUpdate();
                                                    System.out.println("Success!");
                                                } else {
                                                    System.out.println("Not enough money!");
                                                }
                                            }
                                        } catch (SQLException e) {
                                            e.printStackTrace();
                                            connection.rollback();
                                        }
                                    } catch (SQLException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }

                        } else if (action2 == 4) {
                            String closeCard = "DELETE FROM card WHERE number = ?";

                            try (Connection connection = dataSource.getConnection()) {
                                try (PreparedStatement preparedStatement = connection.prepareStatement(closeCard)) {
                                    preparedStatement.setString(1, cardNumber);
                                    preparedStatement.executeUpdate();
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                    connection.rollback();
                                }
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }

                            System.out.println("The account has been closed!");
                            break;
                        } else {
                            System.out.println("Invalid action!");
                        }
                    }
                    if (exit) {
                        break;
                    }
                }
            } else {
                System.out.println("Invalid action");
            }
            System.out.println();
        }
    }

    public static boolean testLuhnAlgorithm(String number) {
        int controlNumber = 0;
        int ch;
        for (int i = 0; i < number.length(); i++) {
            ch = Character.getNumericValue(number.charAt(i));
            if (i % 2 == 0) {
                ch *= 2;
            }
            if (ch > 9) {
                ch -= 9;
            }
            controlNumber += ch;
        }
        return controlNumber % 10 == 0;
    }
}

class BankAccount {

    protected String cardNumber;
    protected String pin;
    protected long balance;
    Random random = new Random();

    public BankAccount() {
        random.setSeed(random.nextInt());
        pin = "" + (random.nextInt(8999) + 1001);
        cardNumber = cardNumberGenerator();
        balance = 0;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public String getPin() {
        return pin;
    }

    private String cardNumberGenerator() {

        random.setSeed(random.nextInt());
        StringBuilder newCardNumber = new StringBuilder();
        newCardNumber.append("400000");
        newCardNumber.append(random.nextInt(89999) + 10001);
        newCardNumber.append(random.nextInt(8999) + 1001);

        int controlNumber = 0;
        int ch;
        int checkSum = 0;
        for (int i = 0; i < newCardNumber.length(); i++) {
            ch = Character.getNumericValue(newCardNumber.charAt(i));
            if (i % 2 == 0) {
                ch *= 2;
            }
            if (ch > 9) {
                ch -= 9;
            }
            controlNumber += ch;
        }

        for (int i = 0; i < 10; i++) {
            if ((controlNumber + i) % 10 == 0) {
                checkSum = i;
                break;
            }
        }

        newCardNumber.append(checkSum);

        return newCardNumber.toString();
    }

}