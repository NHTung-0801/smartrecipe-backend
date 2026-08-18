package com.smartrecipe.smartrecipe_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
@EnableCaching
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

	@Bean
	public CommandLineRunner fixDbEncoding(JdbcTemplate jdbcTemplate) {
		return args -> {
			try {
				// Xóa Trứng ngan đã lưu sai trước đó
				int countDeleted = jdbcTemplate.update("DELETE FROM ingredients WHERE name = 'Trứng ngan'");
				if (countDeleted > 0) System.out.println("Đã xóa " + countDeleted + " dòng Trứng ngan khỏi DB");
				
				int countQua = jdbcTemplate.update("UPDATE ingredients SET base_unit = 'quả' WHERE id = 3 OR base_unit LIKE '%quáº£%'");
				int countCu = jdbcTemplate.update("UPDATE ingredients SET base_unit = 'củ' WHERE base_unit LIKE '%cá»§%'");
				
				int uc1 = jdbcTemplate.update("UPDATE unit_conversions SET from_unit = 'muỗng canh' WHERE from_unit LIKE '%muá»—ng canh%' OR from_unit LIKE '%mu?ng canh%'");
				int uc2 = jdbcTemplate.update("UPDATE unit_conversions SET from_unit = 'muỗng cà phê' WHERE from_unit LIKE '%muá»—ng cÃ  phÃª%' OR from_unit LIKE '%mu?ng c%ph%'");
				int uc3 = jdbcTemplate.update("UPDATE unit_conversions SET from_unit = 'chén' WHERE from_unit LIKE '%chÃ©n%' OR from_unit LIKE '%ch?n%'");
				int uc4 = jdbcTemplate.update("UPDATE unit_conversions SET from_unit = 'bát' WHERE from_unit LIKE '%bÃ¡t%' OR from_unit LIKE '%b?t%'");
				
				System.out.println("FIXED DB ENCODING FOR UNITS: qua=" + countQua + ", uc_fixed=" + (uc1+uc2+uc3+uc4));
			} catch (Exception e) {
				System.err.println("Could not run DB fix: " + e.getMessage());
			}
		};
	}

}
