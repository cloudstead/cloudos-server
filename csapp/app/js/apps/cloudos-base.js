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
		controller.set('message', getAlertMessage(NotificationStack.pop()));

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
			console.log('MESSAGE');
			// var cont = this.controllerFor('flashNotification').set('model',message);

			// this.render('flashNotification', {
			// 	into: 'application',
			// 	outlet: 'notification'
			// });
			// alertify.set('notifier','position', 'bottom-right');
			// alertify.error(message, 0);
			$.notify(message, { position: "bottom-right", autoHideDelay: 10000, className: 'error' });
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
	message: "",
	actions: {
		'select_app': function (app_name) {
			this.transitionToRoute('app', app_name);
		}
	},
	refreshAuthStatus: function() {
		this.set('cloudos_session', CloudOsStorage.getItem('cloudos_session'));
		this.set('cloudos_account', CloudOs.account());
	},

	reInitializeZurb: function() {
		Ember.run.scheduleOnce('afterRender', initialize_zurb);
	}.observes("cloudos_session", "cloudos_account"),
});

App.IndexRoute = Ember.Route.extend({
	model: function() {
		return CloudOs.account().availableApps;
	},
	// beforeModel: function() {
	// 	this.transitionTo('app', 'roundcube');
	// }
});

App.IndexController = Ember.ArrayController.extend({
	cloudos_account: CloudOs.account(),
	username: get_username()
});

App.ApplicationView = Ember.View.extend({
	initFoundation: function () {
		var controller = this.get('controller');
		var message = controller.get("message");
		console.log("msg: ", message);
		if (!Ember.isEmpty(message)){
			controller.send('showFlashMessage', message);
		}
		initialize_zurb();
	}.on('didInsertElement')
});

App.AvailableAppController = Ember.ObjectController.extend({
	icon: function() {
		return this.get('assets.taskbarIconUrl');
	}.property("assets.taskbarIconUrl"),

	appName: function() {
		var appName = this.get("name");
		var appCaption = this.get("assets.taskbarIconAltText");
		console.log("a: ", appName, appCaption);
		if (Ember.isEmpty(appCaption)){
			appCaption = appName;
		}
		return appCaption;
	}.property("assets.taskbarIconAltText")
});
