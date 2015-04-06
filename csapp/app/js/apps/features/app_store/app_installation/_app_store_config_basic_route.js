App.AppstoreConfigBasicRoute = Ember.Route.extend({
	config: { categories: [] },

	app: null,

	items: [],

	model: function(params) {
		this.set("app", App.ConfigAppInfo.all.findBy('name', params.appname));
		var cats = [];
		if (!Ember.isNone(this.get("app"))) {
			this.set("config", Api.read_app_config(this.get("app")));
			if (this.hasConfig()) {
				this.get("config.categories").forEach(function(category) {
					var items = [];
					var item_names = [];
					category.items.forEach(function(item) {
						var value = "";
						if (category.values !== undefined){
							value = category.values[item] || "";
						}
						items.push({
							name: item,
							value: value,
							isPassword: (item.indexOf('password') !== -1) ? true : false
						});
						item_names.push(item);
					});
					cats.push({
						name: category.name,
						items: items,
						item_names: item_names
					});
				});
			}
		}
		return cats;
	},

	afterModel: function(model, transition) {
		if (Ember.isNone(this.get("app"))){
			this.transitionTo('appstore');
		}
	},

	hasConfig: function() {
		return !Ember.isNone(this.get("config")) && !Ember.isEmpty(this.get("config.categories"));
	},

	actions: {
		openModal: function(modalName) {
			this.render(modalName, {
				into: 'application',
				outlet: 'modal'
			});
		},

		closeModal: function(){
			this.disconnectOutlet({
				outlet: 'modal',
				parentView: 'application'
			});
		},

		goToInstall: function(controller) {
			console.log("go to install");
			controller.set("taskId", Api.install_cloud_app(this.get("app")).uuid);
			this.send("openModal", "app_install_modal");
		},

		transitionToAppStore: function() {
			this.transitionTo("appstore");
		},

		refreshInstalledApps: function(){
			this.send("closeModal");

			var account = Api.account_for_token(CloudOsStorage.getItem('cloudos_session'));
			CloudOs.set_account(account);
			this.controllerFor('application').set('cloudos_account', CloudOs.account());

			this.send('transitionToAppStore');
		},
	}
});
