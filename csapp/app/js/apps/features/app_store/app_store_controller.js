App.AppstoreController = Ember.ArrayController.extend({
	appUrl: '',
	currentPage: 1,
	user_message: "Initializing download...",
	user_message_priority: "",
	isHidden: true,
	search: "",

	statusReportClass: function() {
		console.log("hidden: ", this.get("isHidden"));
		var cls = "large-12 columns";
		return cls + (this.get("isHidden") ? " hide" : "");
	}.property("isHidden"),

	reEnteredPage: function(){
		this.set('currentPage', 1);
	}.observes('content'),

	actions: {
		installFromUrl: function () {
			var task_id = Api.install_app_from_url(this.get('appUrl'));
			if (task_id && task_id.uuid) {
				this.transitionTo('tasks', task_id.uuid);
			}
		},

		loadNextPage: function () {
			this.set('currentPage', this.get('currentPage') + 1);
			this.set('content', App.CloudOsApp.loadNextPage(this.get('currentPage'), this.get('search')));
		},

		doOpenApp: function(app) {
			window.location.replace(app.get("openAppUri"));
		},

		doConfigApp: function(app) {
			var self = this;

			var download_data = {
				token: app.id,
				url: app.appVersion.bundleUrl,
				autoInstall: false,
				overwrite: true
			};

			var task_id = Api.download_cloud_app(download_data).uuid;
			self.send('openModal','user_message_modal');

			var statusInterval = setInterval(function(){
				var status = Api.get_task_results(task_id);

				if (status.success){
					var info = App.ConfigAppInfo.createNew(JSON.parse(status.returnValue));
					clearInterval(statusInterval);
					self.send("transitionToConfigApp", info);
				} else if (!Ember.isEmpty(status.events)) {
					var message_key = status.events[status.events.length-1].messageKey;
					self.set('user_message', Em.I18n.translations.task.events[message_key]);
					if (status.error !== undefined) {
						clearInterval(statusInterval);
						self.set("isHidden", false);
						self.set('user_message_priority', 'alert-box alert');
					}
				}

			}, 5000);
		},

		search: function(e){
			console.log("Term for search: ", e);
			this.set("currentPage", 1);
			var paging = $.extend(true, {}, DefaultPagination, {filter: e});
			this.set('content', App.CloudOsApp.findPaginated(paging));
		}
	},


	morePagesAvailable: function() {
		console.log("morePagesAvailable: ", App.CloudOsApp.totalCount > (this.get('currentPage') * DefaultPagination.pageSize), this.get('currentPage'), DefaultPagination.pageSize, App.CloudOsApp.totalCount);
		return App.CloudOsApp.totalCount > (this.get('currentPage') * DefaultPagination.pageSize);
	}.property('currentPage', 'content.@each'),

	appTypes: [],
	appLevels: [],

	init: function () {
		this._super();
		this.appTypes.push(Em.I18n.translations.sections.appstr.types.all);
		this.appTypes.push(Em.I18n.translations.sections.appstr.types.publishers);
		this.appTypes.push(Em.I18n.translations.sections.appstr.types.authors);
		this.appTypes.push(Em.I18n.translations.sections.appstr.types.apps);

		this.appLevels.push(Em.I18n.translations.sections.appstr.levels.app);
		this.appLevels.push(Em.I18n.translations.sections.appstr.levels.cloudos);
		this.appLevels.push(Em.I18n.translations.sections.appstr.levels.init);
		this.appLevels.push(Em.I18n.translations.sections.appstr.levels.system);
	}
});