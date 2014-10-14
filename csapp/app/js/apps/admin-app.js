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
    this.resource('selectedapp');
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

App.AccountsRoute = Ember.Route.extend({
    model: function () {
        return {
            'accounts': Api.list_accounts()
        };
    }
});

App.AccountsController = Ember.ObjectController.extend({
	actions:{
		sortBy: function(property){
			var sacc = this.get('sortedAccounts');
			sacc.set('sortProperties',property);
			sacc.set('sortAscending', !sacc.get('sortAscending'));
		}
	},
	sortedAccounts:function(){
		return Ember.ArrayProxy.createWithMixins(Ember.SortableMixin, {
			  content: this.get('accounts').toArray(),
			  sortProperties: 'name',
			  sortAscending: false
			}) 
		}.property()
});

App.newAccountModel = function () {
    return {
        accountName: '',
        recoveryEmail: '',
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
				recoveryEmail: this.get('recoveryEmail'),
				mobilePhone: this.get('mobilePhone'),
				regularEmail: this.get('regularEmail')
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
				delete account.regularEmail;
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
			account["mobilePhoneCountryCode"] = 1;
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
		
		if (!pattern.test(data["recoveryEmail"])){
			response["recoveryEmail"] = error_msg.email_invalid;
		}
		
		if (!pattern.test(data["regularEmail"])){
			response["regularEmail"] = error_msg.email_invalid;
		}
    	return response;
    },
    toggleSysPassword: function(){
    	this.set('generateSysPassword', !this.get('generateSysPassword'));
    },
    generateSysPassword:true,
    primaryGroups:["Admin","User"],
    selectedGroup:"User",
    twoFactorAuth:true,
    toggleTwoFactorAuth: function(){
    	this.set('twoFactorAuth',!this.get('twoFactorAuth'));
    }
});

App.ManageAccountRoute = Ember.Route.extend({
    model: function (params) {
        return Api.find_account(params.name) || App.newAccountModel();
    }
});

App.ManageAccountController = Ember.ObjectController.extend({
    actions: {
        'doUpdateAccount': function () {
            account = {
                name: this.get('accountName'),
                recoveryEmail: this.get('recoveryEmail'),
                mobilePhone: this.get('mobilePhone'),
                admin: this.get('admin')
            };
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
    }
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
    }
});

App.CertController = App.CertsNewController;

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
