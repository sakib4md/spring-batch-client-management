package com.bank.creditbatch.service;

import com.bank.creditbatch.entity.Client;
import com.bank.creditbatch.repository.ClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class CsvClientImportService {

    private final ClientRepository clientRepository;

    public CsvClientImportService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Transactional
    public int importFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("CSV file is empty");
        }
        if (!file.getOriginalFilename().toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new IllegalArgumentException("Only CSV files are supported");
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            List<List<String>> rows = parseCsv(reader);
            if (rows.isEmpty()) {
                throw new IllegalArgumentException("CSV file has no data");
            }

            List<String> headers = rows.get(0).stream()
                    .map(header -> header.trim().replace("\uFEFF", "").toLowerCase(Locale.ROOT))
                    .toList();
            if (!headers.contains("client_id")) {
                throw new IllegalArgumentException("Required column client_id is missing");
            }

            Map<String, Integer> positions = new HashMap<>();
            for (int index = 0; index < headers.size(); index++) {
                positions.put(headers.get(index), index);
            }

            List<Client> imported = new ArrayList<>();
            for (int rowIndex = 1; rowIndex < rows.size(); rowIndex++) {
                List<String> row = rows.get(rowIndex);
                if (row.stream().allMatch(String::isBlank)) continue;

                String clientId = value(row, positions, "client_id");
                if (clientId.isBlank()) {
                    throw new IllegalArgumentException("Client ID is empty at row " + (rowIndex + 1));
                }

                Client client = clientRepository.findById(clientId).orElse(new Client());
                client.setClientId(clientId);
                client.setClientName(required(row, positions, "client_name", rowIndex));
                client.setIndustry(value(row, positions, "industry"));
                client.setCountry(value(row, positions, "country"));
                client.setAnnualRevenueInr(decimal(row, positions, "annual_revenue_inr", rowIndex));
                client.setCreditScore(integer(row, positions, "credit_score", rowIndex));
                client.setCreditRating(value(row, positions, "credit_rating"));
                client.setCreditExposureInr(decimal(row, positions, "credit_exposure_inr", rowIndex));
                client.setPackageName(value(row, positions, "package", "package_name"));
                client.setOnboardingStatus(value(row, positions, "onboarding_status"));

                String lastReview = value(row, positions, "last_review_date");
                String nextReview = value(row, positions, "next_review_date");
                if (!lastReview.isBlank()) client.setLastReviewDate(LocalDate.parse(lastReview));
                if (!nextReview.isBlank()) client.setNextReviewDate(LocalDate.parse(nextReview));
                imported.add(client);
            }

            if (imported.isEmpty()) {
                throw new IllegalArgumentException("CSV contains no client rows");
            }
            clientRepository.deleteAll();
            clientRepository.saveAll(imported);
            return imported.size();
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read CSV file", exception);
        }
    }

    private List<List<String>> parseCsv(BufferedReader reader) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        int character;
        while ((character = reader.read()) != -1) {
            char current = (char) character;
            if (current == '"') {
                if (quoted && reader.markSupported()) {
                    reader.mark(1);
                    int next = reader.read();
                    if (next == '"') {
                        field.append('"');
                    } else {
                        quoted = false;
                        if (next != -1) reader.reset();
                    }
                } else {
                    quoted = !quoted;
                }
            } else if (current == ',' && !quoted) {
                row.add(field.toString().trim());
                field.setLength(0);
            } else if ((current == '\n' || current == '\r') && !quoted) {
                if (current == '\r') reader.mark(1);
                row.add(field.toString().trim());
                if (row.stream().anyMatch(value -> !value.isBlank())) rows.add(row);
                row = new ArrayList<>();
                field.setLength(0);
            } else {
                field.append(current);
            }
        }
        row.add(field.toString().trim());
        if (row.stream().anyMatch(value -> !value.isBlank())) rows.add(row);
        return rows;
    }

    private String value(List<String> row, Map<String, Integer> positions, String... names) {
        for (String name : names) {
            Integer index = positions.get(name);
            if (index != null && index < row.size()) return row.get(index).trim();
        }
        return "";
    }

    private String required(List<String> row, Map<String, Integer> positions, String name, int rowIndex) {
        String result = value(row, positions, name);
        if (result.isBlank()) throw new IllegalArgumentException("Missing " + name + " at row " + rowIndex);
        return result;
    }

    private BigDecimal decimal(List<String> row, Map<String, Integer> positions, String name, int rowIndex) {
        String result = value(row, positions, name);
        if (result.isBlank()) return null;
        try {
            return new BigDecimal(result);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid " + name + " at row " + (rowIndex + 1));
        }
    }

    private Integer integer(List<String> row, Map<String, Integer> positions, String name, int rowIndex) {
        String result = value(row, positions, name);
        try {
            Integer score = result.isBlank() ? null : Integer.valueOf(result);
            if (score != null && (score < 0 || score > 100)) {
                throw new IllegalArgumentException("Credit score must be between 0 and 100 at row " + (rowIndex + 1));
            }
            return score;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid " + name + " at row " + (rowIndex + 1));
        }
    }
}
