function msg_alert (message) {
	alert(Em.I18n.translations['alerts'][message]);
}

function get_token() {
	return sessionStorage.getItem('cloudos_session') || 'no-token';
}

function add_api_auth (xhr) {
	var token = get_token();
	xhr.setRequestHeader(Api.API_TOKEN, token);
}

Api = {
	API_TOKEN: 'x-cloudos-api-key',

	_get: function (url) {
		var results = null;
		Ember.$.ajax({
			type: 'GET',
			url: url,
			async: false,
			beforeSend: add_api_auth,
			success: function (data, status, jqXHR) {
				results = data;
			}
		});
		return results;
	},

	_update: function (method, url, data) {
		var result = null;
		Ember.$.ajax({
			type: method,
			url: url,
			async: false,
			contentType: 'application/json',
			data: JSON.stringify(data),
			beforeSend: add_api_auth,
			success: function (response, status, jqXHR) {
				result = response;
			},
			error: function (jqXHR, status, error) {
				console.log('setup error: status='+status+', error='+error+', url='+url);
				result = {
					status: status,
					statusCode: jqXHR.status,
					jqXHR: jqXHR,
					errorMessage: error
				};
			}
		});
		return result;
	},

	_post: function(url, data) { return Api._update('POST', url, data); },
	_put:  function(url, data) { return Api._update('PUT', url, data); },

	_delete: function (path) {
		var ok = false;
		Ember.$.ajax({
			type: 'DELETE',
			url: path,
			async: false,
			beforeSend: add_api_auth,
			'success': function (accounts, status, jqXHR) {
				ok = true;
			},
			'error': function (jqXHR, status, error) {
				console.log('error deleting '+path+': '+error);
			}
		});
		return ok;
	},

// first-time setup
	setup: function (setup_key, name, initial_password, new_password, time_zone, mobilePhoneCountryCode, mobilePhone, email, firstName, lastName) {
		var setupData = {
			'setupKey': setup_key,
			'name': name,
			'initialPassword': initial_password,
			'password': new_password,
			'systemTimeZone': time_zone,
			'mobilePhoneCountryCode': mobilePhoneCountryCode,
			'mobilePhone': mobilePhone,
			'email': email,
			'firstName': firstName,
			'lastName' : lastName,
			'admin':true
		};
		return Api._post('/api/setup', setupData);
	},

	restore: function(data) {
		return Api._post("/api/setup/restore", data);
	},

	// admin API
	list_accounts: function () {
		var users = Api._get('/api/accounts');
		return (users == null) ? [] : users;
	},

	find_account: function (account_name) {
		var found = Api._get('/api/accounts/' + account_name);
		return found == null ? { name: account_name } : found;
	},

	add_account: function (account) { return Api._put('/api/accounts/' + account.name, account); },
	update_account: function (account) { return Api._post('/api/accounts/' + account.name, account); },
	delete_account: function (account_name) { return Api._delete('/api/accounts/' + account_name); },

	cloudos_configuration_groups: function () { return Api._get('/api/configs'); },
	cloudos_configuration: function (group) { return Api._get('/api/configs/'+group); },
	cloudos_configuration_value: function (group, name) { return Api._get('/api/configs/'+group+'/'+name).value; },

	install_app_from_url: function (url) { return Api._post('/api/apps', {url: url}); },

	get_task_results: function (task_id) {
		var results = null;
		Ember.$.ajax({
			type: 'GET',
			url: '/api/tasks/' + task_id,
			async: false,
			beforeSend: add_api_auth,
			success: function (data, status, jqXHR) {
				results = data;
			},
			error: function (jqXHR, status, error) {
				results = {
					status: status,
					errorMessage: error,
					jqXHR: jqXHR
				};
			}
		});
		return results;
	},

	list_email_domains: function () { return Api._get('/api/email/domains'); },
	add_email_domain: function (domain) { return Api._put('/api/email/domains/' + domain, null); },
	remove_email_domain: function (domain) { return Api._delete('/api/email/domains/' + domain); },

	find_email_aliases: function () { return Api._get('/api/email/aliases'); },
	find_email_alias: function (alias_name) { return Api._get('/api/email/aliases/' + alias_name); },
	add_email_alias: function (alias) { return Api._put('/api/email/aliases/' + alias.name, alias); },
	edit_email_alias: function (alias) { return Api._post('/api/email/aliases/' + alias.name, alias); },
	remove_email_alias: function (alias_name) { return Api._delete('/api/email/aliases/' + alias_name); },

	get_all_groups: function () { return Api._get('/api/groups'); },
	find_group: function (group_name) { return Api._get('/api/groups/' + group_name); },
	add_group: function (group) { return Api._put('/api/groups/' + group.name, group); },
	edit_group: function (group) { return Api._post('/api/groups/' + group.name, group); },
	delete_group: function (group_name) { return Api._delete('/api/groups/' + group_name); },

	find_ssl_certs: function () {
		var certs = Api._get('/api/security/certs');
		return certs == null ? [] : certs;
	},
	find_ssl_cert: function (name) { return Api._get('/api/security/certs/' + name) },

	add_ssl_cert: function (cert) { return Api._post('/api/security/certs/' + cert.name, cert); },
	remove_ssl_cert: function (cert_name) { return Api._delete('/api/security/certs/' + cert_name); },

	find_apps: function (page) { return Api._post('/api/appstore', page); },

	find_app: function (app_id) { return Api._get('/api/appstore/' + app_id); },

	find_installed_apps: function () { return Api._get('/api/apps/'); },

	install_cloud_app: function (app_id, app_install_request) {
		return Api._post('/api/appstore/' + app_id + '/install', app_install_request); },

	admin_change_password: function (name, newPassword) {
		var request = {
			oldPassword: "-",
			newPassword: newPassword,
			sendInvite: false
		};
		return Api._post('/api/accounts/' + name + '/password', request);
	},

	//
	// Regular user API
	//
	account_for_token: function (token) { return Api._get('/api/sessions/' + token); },

	login_account: function (login) { return Api._post('/api/accounts', login);  },

	change_password: function (name, oldPassword, newPassword) {
		var request = {
			oldPassword: oldPassword,
			newPassword: newPassword
		};
		return Api._post('/api/accounts/' + name + '/password', request);
	},

	change_password_2: function (name, oldPassword, newPassword, uuid, sendInvite) {
		var request = {
			oldPassword: oldPassword,
			newPassword: newPassword,
			uuid: uuid,
			sendInvite: sendInvite
		};
		return Api._post('/api/accounts/' + name + '/password', request);
	},

	forgot_password: function (username) {
		return Api._post('/api/auth/forgot_password/', username);
	},

	reset_password: function (token, password) {
		return Api._post('/api/auth/reset_password/', { "token": token, "password": password });
	},

	get_service_keys: function () {
		return Api._get('/api/security/service_keys/');
	},

	delete_service_key: function (serviceKey) {
		return Api._delete('/api/security/service_keys/' + serviceKey.name);
	},

	check_allow_ssh: function () {
		return Api._get('/api/configs/system/allowssh');
	},

	request_vendor_key: function (key_name) {
		var data = {
			"@class": "rooty.toots.service.ServiceKeyRequest",
			"ctime": Date.now(),
			"errorCount": 0,
			"success": false,
			"finished": false,
			"name": key_name,
			"operation": "GENERATE",
			"recipient": "VENDOR"
		}

		return Api._post('/api/security/service_keys/' + key_name, data);
	},
	}

};
