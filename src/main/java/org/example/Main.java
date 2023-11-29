package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.CollectionType;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {
        try {
            // read data from 'training.txt'
            List<People> people = readDataFromFile("src/main/java/org/example/trainings.txt");

            // 1: List each completed training with a count of how many people have completed that training
            Map<String, Long> trainingCount = countTrainings(people);
            writeToJsonFile("src/main/java/org/example/output1.json", trainingCount);

            // 2: For each specified training, list all people that completed that training in the specified fiscal year
            List<String> specifiedTrainings = Arrays.asList("Electrical Safety for Labs", "X-Ray Safety", "Laboratory Safety Training");
            int fiscalYear = 2024;
            Map<String, List<String>> peopleByTrainingAndYear = filterPeopleByTrainingAndYear(people, specifiedTrainings, fiscalYear);
            writeToJsonFile("src/main/java/org/example/output2.json", peopleByTrainingAndYear);

            // 3: Find all people that have any completed trainings that have expired or will expire within one month
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
            Date targetDate = dateFormat.parse("10/01/2023");
            Map<String, List<String>> expiredTrainings = findExpiredTrainings(people, targetDate);
            writeToJsonFile("src/main/java/org/example/output3.json", expiredTrainings);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<People> readDataFromFile(String filePath) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        CollectionType type = objectMapper.getTypeFactory().constructCollectionType(List.class, People.class);
        return objectMapper.readValue(new File(filePath), type);
    }

    private static void writeToJsonFile(String filePath, Object data) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.writeValue(new File(filePath), data);
    }

    // Task 1
    private static Map<String, Long> countTrainings(List<People> people) {
        return people.stream()
                .flatMap(People -> People.getCompletions().stream())
                .collect(Collectors.groupingBy(Training::getName, Collectors.counting()));
    }

    // Task 2
    private static Map<String, List<String>> filterPeopleByTrainingAndYear(List<People> people, List<String> specifiedTrainings, int fiscalYear) {
        return specifiedTrainings.stream()
                .collect(Collectors.toMap(
                        training -> training,
                        training -> people.stream()
                                .filter(person -> isWithinFiscalYear(person.getCompletions(), training, fiscalYear))
                                .map(People::getName)
                                .collect(Collectors.toList())
                ));
    }

    private static boolean isWithinFiscalYear(List<Training> completions, String trainingName, int fiscalYear) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");

            for (Training completion : completions) {
                if (completion.getName().equals(trainingName)) {
                    Date date = sdf.parse(completion.getTimestamp());
                    Calendar fiscalYearStart = Calendar.getInstance();
                    fiscalYearStart.set(fiscalYear - 1, Calendar.JULY, 1);

                    Calendar fiscalYearEnd = Calendar.getInstance();
                    fiscalYearEnd.set(fiscalYear, Calendar.JUNE, 30);

                    if (date.after(fiscalYearStart.getTime()) && date.before(fiscalYearEnd.getTime())) {
                        return true;
                    }
                }
            }
            return false;
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Task 3
    private static Map<String, List<String>> findExpiredTrainings(List<People> people, Date targetDate) {
        return people.stream()
                .filter(person -> hasExpiredOrExpiresSoon(person.getCompletions(), targetDate))
                .collect(Collectors.toMap(
                        People::getName,
                        person -> person.getCompletions().stream()
                                .filter(training -> isExpiredOrExpiresSoon(training, targetDate))
                                .map(training -> training.getName() + " (" + getExpirationStatus(training, targetDate) + ")")
                                .collect(Collectors.toList())
                ));
    }

    private static boolean hasExpiredOrExpiresSoon(List<Training> completions, Date targetDate) {
        return completions.stream().anyMatch(training -> isExpiredOrExpiresSoon(training, targetDate));
    }

    private static boolean isExpiredOrExpiresSoon(Training training, Date targetDate) {
        if (training.getExpires() == null) {
            return false;
        }

        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
            Date expirationDate = dateFormat.parse(training.getExpires());

            Calendar oneMonthBeforeTarget = Calendar.getInstance();
            oneMonthBeforeTarget.setTime(targetDate);
            oneMonthBeforeTarget.add(Calendar.MONTH, 1);

            return expirationDate.before(oneMonthBeforeTarget.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static String getExpirationStatus(Training training, Date targetDate) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
            Date expirationDate = dateFormat.parse(training.getExpires());

            if (expirationDate.before(targetDate)) {
                return "expired";
            } else {
                Calendar oneMonthBeforeTarget = Calendar.getInstance();
                oneMonthBeforeTarget.setTime(targetDate);
                oneMonthBeforeTarget.add(Calendar.MONTH, 1);

                if (expirationDate.before(oneMonthBeforeTarget.getTime())) {
                    return "expire soon";
                } else {
                    return "not expired";
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
            return "unknown";
        }
    }
}