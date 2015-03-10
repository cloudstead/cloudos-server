App.InstalledappController = Ember.ObjectController.extend({
	isInstalledString: function() {
		return this.get('installed') ?
			Em.I18n.translations.sections.installed_apps.is_installed :
			Em.I18n.translations.sections.installed_apps.is_not_installed;
	}.property("installed"),

	parentString: function() {
		var parent = this.get('parent');
		return Ember.isNone(parent) ? "-" : parent;
	}.property("parent"),
});
