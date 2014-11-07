App = Ember.Application.create({
	// for debugging, disable in prod
	LOG_TRANSITIONS: true,
	LOG_ACTIVE_GENERATION: true
});

App.Router.map(function() {
	this.resource('logout');
//  this.resource('apps');
	this.resource('appstore');
	this.resource('installedapps');
	this.resource('selectedapp', {path: '/appstore/:appname'});
	this.resource('tasks', { path: '/task/:task_id' });
	this.resource('email', function () {
		this.route('domains');
		this.resource('aliases', function() {
			this.route('new');
		});
		this.resource('alias', { path: '/alias/:alias_name' });
	});
	this.resource('accounts');
	this.resource('addAccount');
	this.resource('manageAccount', { path: '/accounts/:name' });

	this.resource('security', function() {
		this.resource('certs', function() {
			this.route('new');
		});
		this.resource('cert', { path: '/cert/:cert_name' });
	});

	this.resource('groups', function () {
		this.resource('delete', { path: '/group/delete/:group_name' });
	});

	this.route('groupsNew', { path: "/groups/new/" });
	this.route('group', { path: '/group/:group_name' });

//  this.resource('addCloud', { path: '/add_cloud/:cloud_type' });
//  this.resource('configCloud', { path: '/cloud/:cloud_name' });
});

App.ApplicationRoute = Ember.Route.extend({
	model: function() {
		return {
			cloudos_session: sessionStorage.getItem('cloudos_session'),
			cloudos_account: CloudOs.account()
		};
	},
	setupController: function(controller, model) {

		// is HTML5 storage even supported?
		if (typeof(Storage) == "undefined") {
			alert('Your browser is not supported. Please use Firefox, Chrome, Safari 4+, or IE8+');
			return;
		}

		// do we have an API token?
		if (!model.cloudos_session) {
			window.location.replace('/index.html');
			return;
		}

		// is the token valid?
		var account = Api.account_for_token(model.cloudos_session);
		if (!account || !account.admin) {
			sessionStorage.removeItem('cloudos_session');
			sessionStorage.removeItem('cloudos_account');
			window.location.replace('/index.html');
			return;
		}

		CloudOs.set_account(account);
		pathArray = window.location.href.split( '/' );
		if (((pathArray[3] == '') || (pathArray[3] == '#') || (pathArray[3] == 'admin.html')) && (!pathArray[4]))
		{
			this.transitionTo('accounts');
		}

	}
});

App.RequestMessagesObject = Ember.Object.extend({
	_doInitialization: function() {
		var self = this;
		self.set("error", this.get('json')["errors"]);
	}.on('init')
});

App.IndexRoute = App.ApplicationRoute;

App.LogoutRoute = Ember.Route.extend({
	setupController: function(controller, model) {
		sessionStorage.clear();
		localStorage.clear();
		window.location.replace('/admin.html');
	}
});

App.ApplicationController = Ember.ObjectController.extend({

	cloudos_session: function () {
		return sessionStorage.getItem('cloudos_session');
	}.property('cloudos_session'),

	cloudos_account: function () {
		return CloudOs.account();
	}.property('cloudos_account'),
	actions: {
		'select_app': function (app_name) {
			window.location.replace('/#/app/' + app_name);
		}
	}

});

App.SelectedappRoute = Ember.Route.extend({

});

App.SelectedappController = Ember.ObjectController.extend({
	actions: {
		select_app: function (app_name) {
			window.location.replace('/#/app/' + app_name);
		}
	}
});

App.InstalledappsRoute = Ember.Route.extend({

});

App.InstalledappsController = Ember.ObjectController.extend({

});

App.AppstoreRoute = Ember.Route.extend({
	model: function() {
		var page = {"pageSize": "10", "pageNumber": "1"};
		var apps = Api.find_apps(page);
		return { "apps": apps };
	}
});

App.AppstoreController = Ember.ObjectController.extend({
	appUrl: '',
	actions: {
		installFromUrl: function () {
			var task_id = Api.install_app_from_url(this.get('appUrl'));
			if (task_id && task_id.uuid) {
				this.transitionTo('tasks', task_id.uuid);
			}
		}
	}
});

App.TasksRoute = Ember.Route.extend({
	model: function(model) {
		return { task_id: model.task_id,
			result: Api.get_task_results(model.task_id) };
	}
});

App.Account = Ember.Object.extend({

	isSelected: false,

	isActive: function(){
		return this.get("suspended") ? false : true;
	}.property("suspended"),

	accountDOMId: function(){
		return this.get("accountName").trim().replace(" ", "_");
	}.property("accountName"),

	changeCheckboxSelect: function(){
		// 'this' here refers to the checkbox object.
		// it's context is acctually this object.
		account = this.get("context");
		account.get("isSelected");
	},

	destroy: function() {
		var account_destroyed = Api.delete_account(this.get("accountName"));

		if (account_destroyed){
			App.Account.removeAccount(this);
		}

		return account_destroyed;
	},

	toggleStatus: function(){
		this._toggle_suspend();
		var success = this._commit_status_change();
		if (!success){
			this._toggle_suspend();
		}
		return success;
	},

	changeCheckboxStatus: function(){
		// 'this' here refers to the checkbox object.
		// it's context is acctually this object.
		var account = this.get("context");
		console.log(account.get("suspended"));
		return account.toggleStatus();
	},

	_commit_status_change: function(){
		return Api.update_account(
			{
				name: this.get("name"),
				firstName: this.get("firstName"),
				lastName: this.get("lastName"),
				email: this.get("email"),
				emailVerified: false,
				password: "qwe123",
				mobilePhone: ""+this.get("mobilePhone"),
				admin: this.get("admin"),
				twoFactor: this.get("twoFactor"),
				mobilePhoneCountryCode: ""+this.get("mobilePhoneCountryCode"),
				suspended: this.get("suspended"),
				accountName: this.get("accountName")
			}
		);
	},

	_toggle_suspend: function() {
		this.set('suspended', !this.get("suspended"));
	}
});

App.Account.reopenClass({
	all: Ember.ArrayProxy.create({content: []}),

	findAll: function() {
		var data = Api.list_accounts();
		App.Account.all.clear();
		data.forEach(function(datum) {
			App.Account.all.pushObject(App.Account.create(datum));
		});
		return App.Account.all;
	},

	findByName: function(account_name){
		return App.Account.create(Api.find_account(account_name));
	},

	bulkToggleStatus: function(accounts){
		var success = true;
		accounts.forEach(function(account){
			success = success && account.toggleStatus();
		});
		return success;
	},

	removeAccount: function(account) {
		App.Account.all.removeObject(account);
	}
});

App.newAccountModel = function () {
	return {
		accountName: '',
		email: '',
		mobilePhone: '',
		admin: false
	};
};

App.AccountsRoute = Ember.Route.extend({
	model: function () {
		return App.Account.findAll();
	}
});

App.AccountFilter = Ember.Object.reopenClass({
	filterSelected: function(accounts) {
		return accounts.filter(function(account){
				return account.get('isSelected');
			});
	}
});

App.AccountsController = Ember.ArrayController.extend({
	actions:{
		sortBy: function(property){
			this.set('sortProperties',property);
			this.set('sortAscending', !this.get('sortAscending'));
		},
		doBulkAction: function(selectorId){
			var bulk_action = $("#" + selectorId).val();
			var selectedAccounts = App.AccountFilter.filterSelected(this.get('arrangedContent'));

			if (bulk_action === "bulk_toggle_status"){
				App.Account.bulkToggleStatus(selectedAccounts);
			}
			else if (bulk_action === "bulk_delete"){
				selectedAccounts.forEach(function(account){
					account.destroy();
				});
			}
		}
	}
});

App.newAccountModel = function () {
	return {
		accountName: '',
		email: '',
		mobilePhone: '',
		admin: false
	};
}

App.AddAccountRoute = Ember.Route.extend({
	model: App.newAccountModel
});

App.AddAccountController = Ember.ObjectController.extend({
	actions: {
		doCreateAccount: function () {

			// this is minimum data for the account
			account = {
				name: this.get('accountName'),
				lastName: this.get('lastName'),
				firstName: this.get('firstName'),
				email: this.get('email'),
				mobilePhone: this.get('mobilePhone'),
			}

			// first check if password is system based, if not, add the passwords for checkup
			if (this.get('generateSysPassword') === false){
				account["password"] = this.get('password');
				account["passwordConfirm"] = this.get('passwordConfirm');
			}

			// validate data
			var validate = this.validate(account);
			var validate_res = true;
			for (var key in validate) {
				value = validate[key];
				if (value != null){
					this.set('requestMessages',
							App.RequestMessagesObject.create({
								json: {"status": 'error', "api_token" : null,
									"errors": validate}
							})
						);
					validate_res =  false;
					}
				}
			if (validate_res === false) { return false; }
			// if validation is success, first remove passConfirm key if exists
			// also, remove regular Email until the api is ready for that
			try{
				delete account.passwordConfirm;
			}catch(e){
				//
			}

			// check if admin, set appropriate key
			if (this.get('selectedGroup') == 'Admin'){
				account["admin"] = true;
			}else{
				account["admin"] = false;
			}

			// check if two-factor, set appropriate key
			if (this.get('twoFactorAuth')){
				account["twoFactor"] = true;
			}else{
				account["twoFactor"] = false;
			}

			// account is not suspended, hc mobile code, accountName is name
			account["suspended"] = false;
			account["mobilePhoneCountryCode"] = this.get("selectedCountry") ? this.get("selectedCountry")["code"] : 1;
			account["accountName"] = account["name"];

			if (Api.add_account(account)) {
				this.transitionTo('accounts');
			}
		},
		cancelCreate: function() {
			if (confirm("Cancel changes ?") == true) {
				this.transitionTo('accounts');
			} else {
				// nada
			}
		}
	},
	validate: function(data){
		var error_msg = locate(Em.I18n.translations, 'errors');
		var pattern = /^(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|(\".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;

		var response = {};

		for (var key in data) {
			value = data[key];
			if ((String(value).trim() == '') || (!value)) {
				response[key] = error_msg.field_required;
			}
		}

		try{
			if (data["password"] != data["passwordConfirm"]){
				response["password"] = error_msg.password_mismatch;
				response["passwordConfirm"] = error_msg.password_mismatch;
			}
		}catch(e){
			//
		}

		if (!pattern.test(data["email"])){
			response["email"] = error_msg.email_invalid;
		}
		return response;
	},
	toggleSysPassword: function(){
		this.set('generateSysPassword', !this.get('generateSysPassword'));
	},
	generateSysPassword:true,
	primaryGroups:["Admin","User"],
	selectedGroup:"User",
	countryList: Countries.list,
	twoFactorAuth:true,
	toggleTwoFactorAuth: function(){
		this.set('twoFactorAuth',!this.get('twoFactorAuth'));
		return false;
	}
});

App.ManageAccountRoute = Ember.Route.extend({
	model: function (params) {
		return App.Account.findByName(params.name);
		// return Api.find_account(params.name) || App.newAccountModel();
	}
});

App.ManageAccountController = Ember.ObjectController.extend({
	actions: {
		'doUpdateAccount': function () {
			account = {
				name: this.get('accountName'),
				firstName: this.get('firstName'),
				lastName: this.get('lastName'),
				email: this.get('email'),
				mobilePhone: this.get('mobilePhone'),
				admin: this.get('admin')
			};

			// check if admin, set appropriate key
			account["admin"] = this.get('selectedGroup') == 'Admin' ? true : false;

			// check if two-factor, set appropriate key
			account["twoFactor"] = this.get('twoFactor');

			account["mobilePhoneCountryCode"] = this.get("selectedCountry")["code"];

			// account is not suspended, hc mobile code, accountName is name
			account["suspended"] = false;
			account["accountName"] = account["name"];

			if (Api.update_account(account)) {
				this.transitionTo('accounts');
			}
		},
		'doDeleteAccount': function (name) {
			if (Api.delete_account(name)) {
				this.transitionTo('accounts');
			}
		},
		cancelCreate: function() {
			if (confirm("Cancel changes ?") == true) {
				this.transitionTo('accounts');
			} else {
				// nada
			}
		}
	},

	primaryGroups: ["Admin","User"],

	countryList: Countries.list,

	selectedGroup: function() {
		return this.get('model').admin ? this.primaryGroups[0] : this.primaryGroups[1];
	}.property("selectedGroup", "model"),

	selectedCountry: function() {
		var countryCode = this.get('model').mobilePhoneCountryCode;

		return Countries.findByCode(countryCode);
	}.property("selectedCountryCode", "model"),
});

App.EmailDomainsRoute = Ember.Route.extend({
	model: function () {
		return {
			'mxrecord': Api.cloudos_configuration().mxrecord,
			'domains': Api.list_email_domains()
		}
	}
});

App.EmailDomainsController = Ember.ObjectController.extend({
	actions: {
		doAddDomain: function () {
			var name = this.get('domain');
			if (!Api.add_email_domain(name)) {
				alert('error adding domain: ' + name);
			}
		},
		doRemoveDomain: function (name) {
			if (!Api.remove_email_domain(name)) {
				alert('error removing domain: ' + name);
			}
		}
	}
});

App.AliasesRoute = Ember.Route.extend({
	model: function () {
		return Api.find_email_aliases();
	}
});

App.AliasesIndexController = Ember.ObjectController.extend({
	actions: {
		doRemoveAlias: function (name) {
			if (!Api.remove_email_alias(name)) {
				alert('error removing alias: '+name);
			}
		}
	}
});

App.AliasesNewController = Ember.ObjectController.extend({
	actions: {
		doAddAlias: function () {
			var name = this.get('aliasName');
			var recipients = [];
			var recipient_names = this.get('recipients').split(",");
			for (var i=0; i<recipient_names.length; i++) {
				recipients.push(recipient_names[i].trim());
			}
			if (!Api.add_email_alias({ 'name': name, 'recipients': recipients })) {
				alert('error adding alias: '+name);
			}
		}
	}
});

App.AliasRoute = Ember.Route.extend({
	model: function (params) {
		var alias = Api.find_email_alias(params['alias_name']);
		var recipients = [];
		for (var i=0; i<alias.members.length; i++) {
			recipients.push(alias.members[i].name);
		}
		return {
			'aliasName': params['alias_name'],
			'recipients': recipients
		}
	}
});

App.AliasController = Ember.ObjectController.extend({
	actions: {
		doEditAlias: function () {
			var name = this.get('aliasName');
			var recipients = [];
			var recipient_names = this.get('recipients').split(",");
			for (var i=0; i<recipient_names.length; i++) {
				recipients.push(recipient_names[i].trim());
			}
			if (!Api.edit_email_alias({ 'name': name, 'recipients': recipients })) {
				alert('error editing alias: '+name);
			}
		}
	}
});

App.SecurityRoute = Ember.Route.extend({
	setupController: function(controller, model) {
		this.transitionTo('certs.index');
	}
});

App.CertsRoute = Ember.Route.extend({
	model: function () {
		return Api.find_ssl_certs();
	}
});

App.CertsIndexController = Ember.ObjectController.extend({
	actions: {
		doRemoveCert: function (name) {
			if (!Api.remove_ssl_cert(name)) {
				alert('error removing cert: '+name);
			}
		}
	}
});

App.CertsNewController = Ember.ObjectController.extend({
	actions: {
		doAddCert: function () {
			var name = this.get('certName');
			var description = this.get('description');
			var pem = this.get('pem');
			var key = this.get('key');
			var cert = {
				'name': name,
				'description': description,
				'pem': pem,
				'key': key
			};
			if (!Api.add_ssl_cert(cert)) {
				alert('error adding cert: '+name);
			}
		}
	}
});

App.CertRoute = Ember.Route.extend({
	model: function (params) {
		var cert = Api.find_ssl_cert(params['cert_name']);
		return  cert;
	},
});

App.CertController = App.CertsNewController;


App.GroupMembers = Ember.Object.extend({});

App.GroupMembers.reopenClass({
	toArrayFromString: function(string){
		if(string === undefined || string.length === 0){
			return [];
		}
		var recipient_names = string.split(",");
		return recipient_names.map(function(name){
			return name.trim();
		});
	},

	toStringFromArray: function(memberArray){
		if(memberArray === undefined || memberArray.length === 0){
			return "";
		}
		return memberArray.map(function(member) {
			return member.name;
		}).join(", ");
	},
});

App.Group = Ember.Object.extend({
	save: function() {
		var group_commited = this._commit_add();

		if (group_commited){
			App.Group.addGroup(this);
		}

		return group_commited;
	},

	updateWith: function(data) {
		var group_commited = this._commit_edit(data);

		if (group_commited){
			this.modifyWith(data);
		}

		return group_commited;
	},

	destroy: function() {
		var group_commited = this._commit_delete();

		if (group_commited){
			App.Group.removeGroup(this);
		}

		return group_commited;
	},

	modifyWith: function(data){
		this.set('name', data.name);
		this.set('recipients', data.recipients);
	},

	_commit_add: function() {
		var group_commited = Api.add_group(
			{
				name: this.name,
				recipients: this.recipients,
				info: this.info
			}
		);

		return group_commited;
	},

	_commit_edit: function(data) {
		var group_commited = Api.edit_group(
			{
				name: data.name,
				recipients: data.recipients
			}
		);

		return group_commited;
	},

	_commit_delete: function() {
		var group_commited = Api.delete_group(this.name);

		return group_commited;
	}
});

App.Group.reopenClass({
	all: Ember.ArrayProxy.create({content: []}),

	findAll: function() {
		var data = Api.get_all_groups();
		App.Group.all.clear();
		data.forEach(function(datum) {
			App.Group.all.pushObject(App.Group.create(datum));
		});
		return App.Group.all;
	},

	findByName: function(group_name) {
		var group_data = Api.find_group(group_name);

		return App.Group.create(
			{
				name: group_data.name,
				recipients: group_data.members
			}
		);
	},

	getByName: function(group_name){
		return App.Group.all.findBy('name', group_name)
	},

	addGroup: function(group) {
		App.Group.all.pushObject(group);
	},

	removeGroup: function(group) {
		App.Group.all.removeObject(group);
	}
});


App.GroupsRoute = Ember.Route.extend({
	model: function () {
		return App.Group.findAll();
	}
});

App.GroupsController = Ember.ArrayController.extend({
	sortedGroups: function(){
		return this.get('arrangedContent');
	}.property('arrangedContent.@each'),

	actions: {
		doDeleteGroup: function (group_name) {
			var group = App.Group.getByName(group_name);
			if (!group.destroy()){
				this._handleGroupDeleteFailed(group);
			}
		}
	},

	_handleGroupDeleteFailed: function(group) {
		alert('error deleting group: ' + group.name);
	}
});


App.GroupsNewRoute = Ember.Route.extend({
	model: function() {
		return this.modelFor('groups');
	}
});

App.GroupsNewController = Ember.Controller.extend({
	actions: {
		doAddGroup: function () {
			var new_group = App.Group.create(this._formData());

			// validate data
			var groupErrors = Validator.validateGroup(new_group);

			if (groupErrors.is_not_empty){
				this._handleGroupValidationError(groupErrors);
			}
			else{
				new_group.save() ?
					this.send('doReturnToGroups') :
					this._handleGroupSaveFailed(new_group);
			}
		},

		doCancel: function () {
			this.send('doReturnToGroups');
		},

		doReturnToGroups: function() {
			this.transitionToRoute('groups');
		}
	},

	_formData: function() {
		return {
			name: this.get('name'),
			recipients: App.GroupMembers.toArrayFromString(this.get('recipients')),
			info: {
				storageQuota: this.get('storageQuota'),
				description: this.get('description')
			}
		};
	},

	_handleGroupValidationError: function(error) {
		this.set('requestMessages',
			App.RequestMessagesObject.create({
				json: {
					"status": 'error',
					"api_token" : null,
					"errors": error
				}
			})
		);
	},

	_handleGroupSaveFailed: function(group) {
		alert('error adding group: ' + group.name);
	}
});


App.GroupRoute = Ember.Route.extend({
	model: function (params) {
		var group_data = App.Group.findByName(params['group_name']);

		return App.Group.create(
			{
				name: group_data.get('name'),
				recipients: App.GroupMembers.toStringFromArray(group_data.get('recipients'))
			}
		);
	}
});

App.GroupController = Ember.ObjectController.extend({
	actions: {
		doEditGroup: function () {
			var group = this.get('model');

			// validate data
			var groupErrors = Validator.validateGroup(group);

			if (groupErrors.is_not_empty){
				this._handleGroupValidationError(groupErrors);
			}
			else{
				group.updateWith(this._formData()) ?
					this.transitionToRoute('groups') :
					this._handleGroupUpdateFailed(group);
			}
		},

		doCancel: function () {
			this.transitionToRoute('groups');
		}
	},

	_formData: function() {
		return {
			name: this.get('name'),
			recipients: App.GroupMembers.toArrayFromString(this.get('recipients'))
		};
	},

	_handleGroupValidationError: function(error) {
		this.set('requestMessages',
			App.RequestMessagesObject.create({
				json: {
					"status": 'error',
					"api_token" : null,
					"errors": error
				}
			})
		);
	},

	_handleGroupUpdateFailed: function(group) {
		alert('error updating group: ' + group.name)
	}
});


Ember.Handlebars.helper('cloud-type-field', function(cloudType, field) {

	var cloudTypeTranslations = Em.I18n.translations['cloudTypes'][cloudType];
	if (!cloudTypeTranslations) return '??undefined translation: cloudTypes.'+cloudType+'.'+field;

	var name = cloudTypeTranslations[field];
	if (!name) return '??undefined translation: cloudTypes.'+cloudType+'.'+field;

	return new Handlebars.SafeString(name);
});

Ember.Handlebars.helper('cloud-option-name', function(cloudType, optionName) {
	var cloudTypeTranslations = Em.I18n.translations['cloudTypes'][cloudType];
	if (!cloudTypeTranslations) return '??undefined translation: cloudTypes.'+cloudType+'.'+field;

	var name = cloudTypeTranslations['options'][optionName];
	if (!name) return '??undefined translation: cloudTypes.'+cloudType+'.options.'+optionName;

	return name;
});

Ember.Handlebars.helper('app-usage', function(usage) {
	var appUsage = Em.I18n.translations['appUsage'][usage];
	if (!appUsage) return '??undefined translation: appUsage.'+usage;
	return appUsage;
});

Ember.Handlebars.helper('task-description', function(result) {
	var action = Em.I18n.translations['task'][result.actionMessageKey];
	if (!action) return '??undefined translation: task.'+result.actionMessageKey;
	if (result.target) action += ": " + result.target;
	return action;
});

Ember.Handlebars.helper('task-event', function(key) {
	var value = Em.I18n.translations['task']['events'][key];
	if (!value) return '??undefined translation: task.events.'+key;
	return value;
});

Ember.Handlebars.helper('app-name', function(name) {
	var appName = Em.I18n.translations['appNames'][name];
	if (!appName) return '??undefined translation: appNames.'+name;
	return appName;
});

Ember.Handlebars.helper('fromUTC', function(timeUTC) {
	var convertedDate = new Date(0);
	convertedDate.setUTCSeconds(Math.round(timeUTC/1000));
	return convertedDate.toLocaleString();
});

Ember.Handlebars.helper('getStatus', function(account) {
	var stat_name = Em.I18n.translations['sections'].acct.status;
	if (account.suspended === true){return stat_name.suspended;}
	if (account.admin === true) {return stat_name.admin;}
	return stat_name.active;
});

App.ApplicationView = Ember.View.extend({
	initFoundation: function () {
		Ember.$(document).foundation();
	}.on('didInsertElement')
});
