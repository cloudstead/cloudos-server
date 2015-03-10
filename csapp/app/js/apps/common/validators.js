Validator = {
	validateTwoFactorVerificationCode: function(code){
		var errors = {"verificationCode": null};
		var error_msg = locate(Em.I18n.translations, 'errors');
		var codeRegexp = /^(\d){7}$/;

		if ((!code) || (code.trim() == '')){
			errors.verificationCode = error_msg.field_required;
		}
		else if (!codeRegexp.test(code)){
			errors.verificationCode = error_msg.two_factor_code_invalid;
		}
		return errors;
	},

	validateGroup: function(group){
		var error_msg = locate(Em.I18n.translations, 'errors');

		var errors = {is_not_empty: false};

		["name", "recipients"].forEach(function(property){
			if(Validator.is_empty(group.get(property))){
				errors[property] = error_msg.field_required;
				errors.is_not_empty = true;
			}
		});

		return errors;
	},

	is_empty: function(value) {
		return (value === undefined || String(value).trim().length === 0) ? true : false;
	}
};

AccountValidator = {
	getValidationErrorsFor: function(account) {
		var data = new ValidatorData(locate(Em.I18n.translations, 'errors'), account);

		data.errors = this._info_fields_errors(data);

		data.errors = account.generateSysPassword ?
			data.errors :
			PresenceValidator.getErrors(data, ["password", "passwordConfirm"]);

		data.errors = EqualPasswordsValidator.getErrors(data, "password", "passwordConfirm");

		data.errors = EmailValidator.getErrors(data, account.get("email"));

		data.errors = PhoneNumberValidator.getErrors(data, account.get("mobilePhone"));

		return data.errors;
	},

	getPasswordValidationErrorsFor: function(account){
		var data = new ValidatorData(locate(Em.I18n.translations, 'errors'), account);

		data.errors = PresenceValidator.getErrors(data, ["newPassword"]);

		return data.errors;
	},

	getUpdateValidationErrorsFor: function(account){
		var data = new ValidatorData(locate(Em.I18n.translations, 'errors'), account);

		data.errors = this._info_fields_errors(data);

		data.errors = EmailValidator.getErrors(data, account.get("email"));

		data.errors = PhoneNumberValidator.getErrors(data, account.get("mobilePhone"));

		return data.errors;
	},

	_info_fields_errors: function(data) {
		return PresenceValidator.getErrors(
				data,
				["name", "firstName", "lastName", "email", "mobilePhone"]
			);
	}
};

PasswordValidator = {
	getErrorsFor: function(object, password, confirm){
		var data = new ValidatorData(locate(Em.I18n.translations, 'errors'), object);

		data.errors = EqualPasswordsValidator.getErrors(data, password, confirm);

		data.errors = PresenceValidator.getErrors(data, [password, confirm]);

		return data.errors;
	}
};

ValidatorData = function(error_msg, validationSubject) {
	this.errors = {
		is_not_empty: false
	};

	this.error_msg = error_msg;

	this.validationSubject = validationSubject;
};

PresenceValidator = {
	getErrors: function(data, fields) {
		fields.forEach(function(property){
			if(EmptyValidator.is_empty(data.validationSubject.get(property))){
				data.errors[property] = data.error_msg.field_required;
				data.errors["is_not_empty"] = true;
			}
		});

		return data.errors;
	},
	validate: function(subject, fields) {
		return this.getErrors(
			new ValidatorData(locate(Em.I18n.translations, 'errors'), subject),
			fields
		);
	}
};

EmptyValidator = {
	is_empty: function(value) {
		return (Ember.isNone(value) || String(value).trim().length === 0);
	}
};

EqualPasswordsValidator = {
	getErrors: function(data, passwordField, confirmField) {
		password = data.validationSubject.get(passwordField);
		confirm = data.validationSubject.get(confirmField);

		if (password !== confirm){
			data.errors[passwordField] = data.error_msg.password_mismatch;
			data.errors[confirmField] = data.error_msg.password_mismatch;
			data.errors["is_not_empty"] = true;
		}
		return data.errors;
	}
};

EmailValidator = {
	getErrors: function(data, email) {
		var pattern = /^(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|(\".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
		if (!pattern.test(email)){
			data.errors["email"] = data.error_msg.email_invalid;
			data.errors["is_not_empty"] = true;
		}
		return data.errors;
	}
};

PhoneNumberValidator = {
	getErrors: function(data, number) {
		var pattern = /^[0-9]([0-9]|[\s]|[-])*$/;
		if (!pattern.test(number)){
			data.errors["mobilePhone"] = data.error_msg.not_a_phone_number;
			data.errors["is_not_empty"] = true;
		}
		return data.errors;
	}
};
