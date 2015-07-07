

App.CloudOsApp = Ember.Object.extend({

	version: function() {
		return this.get("version");
	}.property(),

	statusCaption: function() {
		return Em.I18n.translations.sections.appstr.install_status[this.get("installStatus")];
	}.property("installStatus"),

	isInstalled: function() {
		return this.get('installStatus') == 'installed';
	}.property(),

	isInteractive: function() {
		return this.get('isInstalled') && this.get('interactive');
	}.property(),

	smallIcon: function() {
		return this.get('assets').smallIconUrl || "/images/default_app.png";
	}.property(),

	isUnavailable: function() {
		return this.get('installStatus') == 'unavailable';
	}.property(),

	isAvailableAppStore: function() {
		return this.get('installStatus') == 'available_appstore';
	}.property(),

	isAvailableLocal: function() {
		return this.get('installStatus') == 'available_local';
	}.property(),

	isUpgradeAvailableInstalled: function() {
		return this.get('installStatus') == 'upgrade_available_installed';
	}.property(),

	isUpgradeAvailableNotInstalled: function() {
		return this.get('installStatus') == 'upgrade_available_not_installed';
	}.property(),

	canBeInstalled: function() {
		return this.get("isAvailableAppStore") || this.get("isAvailableLocal") ||
			this.get("isUpgradeAvailableNotInstalled");
	}.property(),

	canBeUpgraded: function() {
		return this.get("isUpgradeAvailableInstalled");
	}.property()
});

App.CloudOsApp.reopenClass({
	all: Ember.ArrayProxy.create({content: []}),

	totalCount: 0,

	findPaginated: function(paging) {
		App.CloudOsApp.all.clear();
		App.CloudOsApp._fetchApps(paging);
		return App.CloudOsApp.all;
	},

	loadNextPage: function(page, filter) {
		var paging = $.extend(true, {}, DefaultPagination, { pageNumber: page, filter: filter});

		App.CloudOsApp._fetchApps(paging);

		return App.CloudOsApp.all;
	},

	_fetchApps: function(paging){
		var data = Api.find_apps(paging);
		App.CloudOsApp.totalCount = data.totalCount;
		var apps = data.results;

		if(Ember.isNone(apps)){ return; }

		apps.forEach(function(app) {
			App.CloudOsApp.all.pushObject(App.CloudOsApp.create(app));
		});
	}
});