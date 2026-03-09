package org.example.catalog.exception;

public class DuplicateCategoryNameException extends RuntimeException {
    public DuplicateCategoryNameException(String name) {
        super("Category with name already exists: " + name);
    }
}
