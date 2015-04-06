App = Ember.Application.create({
	LOG_TRANSITIONS: true // for debugging, disable in prod
});

App.Router.map(function() {
	this.resource('login');
	this.resource('logout');
	this.resource('settings');
	this.resource('app', { path: '/app/:app_name' });

	this.resource('profile', function(){
		this.route('changePassword', { path: '/change_password' });
	});

	this.resource('resetPassword', { path: '/reset_password/:token' });
});

App.ApplicationRoute = Ember.Route.extend({
	model: function() {
		return {
			cloudos_session: CloudOsStorage.getItem('cloudos_session'),
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
			this.transitionTo('login');
			return;
		}

		// is the token valid?
		var account = Api.account_for_token(model.cloudos_session);
		if (!account) {
			CloudOs.logout();
			this.transitionTo('login');
			return;
		}

		CloudOs.set_account(account);
	},
	actions:{
		openModal: function(modalName, model){
			this.controllerFor(modalName).set('model',model);
			return this.render(modalName, {
				into: 'application',
				outlet: 'modal'
			});
		},
		closeModal: function(){
			return this.disconnectOutlet({
				outlet: 'modal',
				parentView: 'application'
			});
		},
		showFlashMessage: function (message) {
			var cont = this.controllerFor('flashNotification').set('model',message);

			this.render('flashNotification', {
				into: 'application',
				outlet: 'notification'
			});
		},
		hideFlashMessage: function () {
			return this.disconnectOutlet({
				outlet: 'notification',
				parentView: 'application'
			});
		},
	}
});

App.ApplicationController = Ember.ObjectController.extend({
	cloudos_session: CloudOsStorage.getItem('cloudos_session'),
	cloudos_account: CloudOs.account(),
	actions: {
		'select_app': function (app_name) {
			this.transitionToRoute('app', app_name);
		}
	}
});

App.IndexRoute = Ember.Route.extend({
	beforeModel: function() {
		this.transitionTo('app', 'roundcube');
	}
});

App.IndexController = Ember.ObjectController.extend({
	cloudos_account: CloudOs.account(),
	username: get_username()
});

App.ApplicationView = Ember.View.extend({
	initFoundation: initialize_zurb.on('didInsertElement')
});
