App = Ember.Application.create({
	// for debugging, disable in prod
	LOG_TRANSITIONS: true,
	LOG_ACTIVE_GENERATION: true
});

App.Router.map(function() {
	this.resource('restore');
	this.resource('keys')
});

App.ApplicationController = Ember.ObjectController.extend({});

App.IndexRoute = Ember.Route.extend({
});

App.RequestMessagesObject = Ember.Object.extend({
	_doInitialization: function() {
		var self = this;
		self.set("error", this.get('json')["errors"]);
	}.on('init')
});

App.IndexController = Ember.ObjectController.extend({

	setup_key: function() {
		try{
			return getParameterByName('key');
		}catch(err){
			//
		}
	}.property(),
	name: '',
	initialPassword: '',
	password: '',
	password2: '',
	content: {},
	mobilePhoneCountryCode:'',
	mobilePhone:'',
	email:'',
	firstName:'',
	lastName:'',
	setup_response: null,
	timeZones: function() {
		var time = new Date();
		var tzoffset = time.getTimezoneOffset();
		for (var i = 0; i < timeZoneList.length; i++) {
			if (tzoffset*-1 == timeZoneList[i].ioffset) {
				timeZoneList[i].dfault = true;
				break;
			}
		}
		return timeZoneList;
	}.property(),
	actions: {
		doSetup: function () {
			var setupKey = this.get('setup_key');
			var name = this.get('name');
			var initial_password = this.get('initialPassword');
			var password = this.get('password');
			var password2 = this.get('password2');
			var error_msg = locate(Em.I18n.translations, 'errors');
			var tzone = $('#setupTZfield').find(":selected")[0].index;
			var mobilePhoneCountryCode = this.get('mobilePhoneCountryCode');
			var mobilePhone = this.get('mobilePhone');
			var email = this.get('email');
			var firstName = this.get('firstName');
			var lastName = this.get('lastName');

			var validate = this.validateSetup(
				name, initial_password, password, password2, mobilePhoneCountryCode, mobilePhone, email, firstName, lastName);
			if ( (validate.account_name) || (validate.cs_password) || (validate.password) || (validate.confirm_password)
					|| validate.mobilePhoneCountryCode || validate.mobilePhone || validate.email || validate.firstName || validate.lastName){

				this.set('requestMessages',
						App.RequestMessagesObject.create({
							json: {"status": 'error', "api_token" : null,
								"errors":
									{"name": validate.account_name,
									"initialPassword": validate.cs_password,
									"password": validate.password,
									"password2": validate.confirm_password,
									"mobilePhoneCountryCode": validate.mobilePhoneCountryCode,
									"mobilePhone": validate.mobilePhone,
									"email": validate.email,
									"firstName": validate.firstName,
									"lastName": validate.lastName}}
					  })
					);
				return false;
			}

			// this.set("setup_response", { restoreKey: "some uuid" });

			// this.transitionToRoute('keys');

			var auth_response = Api.setup(setupKey, name, initial_password, password, tzone, mobilePhoneCountryCode, mobilePhone, email, firstName, lastName);
			if (auth_response) {
				CloudOs.login(auth_response);
				this.set("setup_response", { restoreKey: auth_response.restoreKey });
				this.transitionToRoute('keys');
				// window.location.replace('/admin.html');
			} else {
				alert('error, perhaps the key was not correct. check your email again.');
			}
		}
	},
	validateSetup: function(account_name, cs_password, password, confirm_password, mobilePhoneCountryCode, mobilePhone, email, firstName, lastName) {
		var response = {"account_name":null,
						"cs_password":null,
						"password":null,
						"confirm_password":null,
						"mobilePhoneCountryCode":null,
						"mobilePhone":null,
						"email":null,
						"firstName":null,
						"lastName":null};

		var error_msg = locate(Em.I18n.translations, 'errors');
		var pattern = /^[a-z][a-z0-9]+$/i;

		if ((account_name.trim() == '') || (!account_name)){
			response.account_name = error_msg.field_required;
		}else if(!pattern.test(account_name)){
			response.account_name = error_msg.account_name_invalid;
		}
		if ((cs_password.trim() == '') || (!cs_password)){
			response.cs_password = error_msg.field_required;
		}
		if ((password.trim() == '') || (!password)){
			response.password = error_msg.field_required;
		}else if(password.length < 8) {
			response.password = error_msg.password_short;
		}
		if ((confirm_password.trim() == '') || (!confirm_password)){
			response.confirm_password = error_msg.field_required;
		}
		if (password != confirm_password) {
			response.confirm_password = error_msg.password_mismatch;
		}
		if ((mobilePhoneCountryCode.trim() == '') || (!mobilePhoneCountryCode)){
			response.mobilePhoneCountryCode = error_msg.field_required;
		}
		if ((mobilePhone.trim() == '') || (!mobilePhone)){
			response.mobilePhone = error_msg.field_required;
		}
		if ((email.trim() == '') || (!email)){
			response.email = error_msg.field_required;
		}
		if ((firstName.trim() == '') || (!firstName)){
			response.firstName = error_msg.field_required;
		}
		if ((lastName.trim() == '') || (!lastName)){
			response.lastName = error_msg.field_required;
		}

		return response;
	}
});

App.RestoreRoute = Ember.Route.extend({
	model: function() {
		return {};
	},
	actions:{
		openModal: function(modalName){
			return this.render(modalName, {
				into: 'application',
				outlet: 'modal'
			});
		},
		closeModal: function(){
			this.controllerFor('restore').set("isHidden", true);
			return this.disconnectOutlet({
				outlet: 'modal',
				parentView: 'application'
			});
		}
	}
});

App.RestoreController = Ember.ObjectController.extend({
	setup_key: function() {
		try{
			return getParameterByName('key');
		}catch(err){
			//
		}
	}.property(),

	user_message_priority: "alert-box",

	user_message: "Initializing restore...",

	isHidden: true,

	actions: {
		doRestore: function(){
			var self = this;

			var restore_data = {
				setupKey: this.get('setup_key'),
				initialPassword: this.get('initialPassword'),
				restoreKey: this.get('restoreKey'),
				notifyEmail: this.get('notifyEmail')
			};

			var validate = this.validate(restore_data);

			if (this.hasValidationFailed(validate)){
				this.set('requestMessages',
							App.RequestMessagesObject.create({
								json: {
									status: 'error',
									api_token : null,
									errors: validate
								}
							})
						);
			}
			else {
				var task_id = Api.restore(restore_data).uuid;
				self.send('openModal','user_message_modal');

				var statusInterval = setInterval(function(){
					result = Api.get_task_results(task_id);

					if (self.hasRestoreTaskFailed(result)){
						var last_error = result.events[result.events.length-1].messageKey
						self.set("user_message", Em.I18n.translations.task.events[last_error]);
						self.set("user_message_priority", "alert-box alert");
						self.set("isHidden", false);
					}
					else if (self.isShuttingDown(result)){
						self.set("user_message", Em.I18n.translations.task.events['{restore.shutting_down}']);
						self.set("user_message_priority", "alert-box");
					}
					else if (self.hasRestoreTaskSucceded(result)){
						window.clearInterval(statusInterval);
						self.set("user_message", Em.I18n.translations.task.events['{restore.done}']);
						self.set("user_message_priority", "alert-box success");
					}
					else{
						self.set("user_message", Em.I18n.translations.task.events[result.actionMessageKey]);
						self.set("user_message_priority", "alert-box");
					}
				}, 5000);
			}
		}
	},

	validate: function(data) {
		var error_msg = locate(Em.I18n.translations, 'errors');
		var response = {
			setupKey:null,
			initialPassword:null,
			notifyEmail:null,
			restoreKey:null,
		};

		if (Ember.isEmpty(data.setupKey)){
			response.setupKey = error_msg.setup_key_missing;
		}
		if (Ember.isEmpty(data.initialPassword)){
			response.initialPassword = error_msg.field_required;
		}else if(data.initialPassword.length < 8) {
			response.initialPassword = error_msg.password_short;
		}
		if (Ember.isEmpty(data.notifyEmail)){
			response.notifyEmail = error_msg.field_required;
		}
		if (Ember.isEmpty(data.restoreKey)){
			response.restoreKey = error_msg.field_required;
		}

		return response;
	},

	hasValidationFailed: function(validation) {
		return !Ember.isNone(validation.setupKey) || !Ember.isNone(validation.restoreKey) ||
			!Ember.isNone(validation.initialPassword) || !Ember.isNone(validation.notifyEmail);
	},

	hasRestoreTaskSucceded: function(task) {
		return task.status === 'error' && task.jqXHR.status == 404;
	},

	hasRestoreTaskFailed: function(task) {
		return !Ember.isEmpty(task.error);
	},

	isShuttingDown: function(task) {
		return task.status === 'error' && task.jqXHR.status == 503;
	}
});


App.KeysRoute = Ember.Route.extend({
	model: function() {
		return {};
	}
});

App.KeysController = Ember.ObjectController.extend({
	needs: "index",

	restoreKey: Ember.computed.alias("controllers.index.setup_response.restoreKey"),

	actions: {
		doComplete: function(){
			window.location.replace('/admin.html');
		}
	},
});

App.KeysView = Ember.View.extend({

	didInsertElement: function(event) {
		Ember.run.next(function(){
			$('textarea').focus();
			$('textarea').select();
		});
	}
});
