package com.tstorm.seed;

import java.util.Optional;

public class Name {
    /**
     * Finds an actor's name suffix, which is IMDb's way of making actor records unique
     */
    private static final String SUFFIX_PATTERN = ".*\\([IVXLCDM]*\\).*";

    private String firstName;
    private Optional<String> lastName = Optional.empty();
    private Optional<String> nickName = Optional.empty();
    private Optional<String> suffix = Optional.empty();

    public Name(String line) {
        String namePart = "";
        try {
            namePart = line.substring(0, line.indexOf("\t"));
        } catch (StringIndexOutOfBoundsException e) {
            throw new RuntimeException();
        }
        if (namePart.substring(0, 1).equals("'")) {
            // nicknames are enclosed in single quotes like 'so'
            nickName = Optional.of(namePart.substring(namePart.substring(1).indexOf("'") + 1));
            namePart = namePart.substring(nickName.get().length() + 1); // cut out the nickname from the line
        }
        int firstNameStartIndex;
        if (namePart.contains(",")) {
            lastName = Optional.of(namePart.substring(0, namePart.indexOf(",")));
            firstNameStartIndex = lastName.get().length() + ", ".length();
            // The first name will be between the last name and the first title (which is seperated by a tab)
            firstName = namePart.substring(firstNameStartIndex).split("\t")[0];
        } else {
            // last name is first and only name
            firstName = namePart.split("\t")[0];
        }
        // first name might contain the suffix
        if (firstName.matches(SUFFIX_PATTERN)) {
            int indexOfFirstParen = firstName.indexOf("(");
            this.suffix = Optional.of(firstName.substring(indexOfFirstParen + 1, firstName.indexOf(")")));
            // ...and fix the first name
            this.firstName = firstName.substring(0, indexOfFirstParen - 1);
        }
    }

    public String getFirstName() {
        return firstName;
    }

    public Optional<String> getLastName() {
        return lastName;
    }

    public Optional<String> getNickName() {
        return nickName;
    }

    public Optional<String> getSuffix() {
        return suffix;
    }
}
