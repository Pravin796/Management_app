package com.pravin.maintenance_app;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class QueryDB {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://localhost:5432/residency_management";
        String user = "postgres";
        String password = "Pravin@69"; 

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {
            
            System.out.println("--- cash_payment_requests ---");
            ResultSet rs = stmt.executeQuery("SELECT id, room_id, amount, status FROM cash_payment_requests");
            while (rs.next()) {
                System.out.println("id: " + rs.getLong("id") + ", room_id: " + rs.getLong("room_id") + ", amount: " + rs.getBigDecimal("amount") + ", status: " + rs.getString("status"));
            }
            
            System.out.println("\n--- payments ---");
            rs = stmt.executeQuery("SELECT id, room_id, amount, method, status FROM payments");
            while (rs.next()) {
                System.out.println("id: " + rs.getLong("id") + ", room_id: " + rs.getLong("room_id") + ", amount: " + rs.getBigDecimal("amount") + ", method: " + rs.getString("method") + ", status: " + rs.getString("status"));
            }
            
            System.out.println("\n--- payment_allocations ---");
            rs = stmt.executeQuery("SELECT id, payment_id, maintenance_id, amount FROM payment_allocations");
            while (rs.next()) {
                System.out.println("id: " + rs.getLong("id") + ", payment_id: " + rs.getLong("payment_id") + ", maintenance_id: " + rs.getLong("maintenance_id") + ", amount: " + rs.getBigDecimal("amount"));
            }
            
            System.out.println("\n--- maintenance ---");
            rs = stmt.executeQuery("SELECT id, room_id, billing_month, amount, status FROM maintenance");
            while (rs.next()) {
                System.out.println("id: " + rs.getLong("id") + ", room_id: " + rs.getLong("room_id") + ", billing_month: " + rs.getString("billing_month") + ", amount: " + rs.getBigDecimal("amount") + ", status: " + rs.getString("status"));
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
