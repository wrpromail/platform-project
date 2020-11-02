package net.coding.lib.project.form;

import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

public abstract class BaseForm implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return this.getClass().equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
    }

    public boolean validate(Errors errors) {
        validate(this, errors);

        return !errors.hasErrors();
    }

    public void rejectValue(Errors errors, String field, String key) {
        errors.rejectValue(field, field, key);
    }

    public void rejectValueWithArgs(Errors errors, String field, String key, Object[] errorArgs) {
        errors.rejectValue(field, field, errorArgs, key);
    }

    public void rejectIfEmptyOrWhitespace(Errors errors, String field, String key) {
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, field, key, key);
    }

    public void rejectIfEmpty(Errors errors, String field, String key) {
        ValidationUtils.rejectIfEmpty(errors, field, key, key);
    }
}
