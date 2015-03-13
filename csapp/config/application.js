module.exports = function(lineman) {

	get_feature = function(feature_name) {
		return "app/js/apps/features/" + feature_name + "/**/*.js";
	}

	get_template = function(template_name) {
		return "app/templates/" + template_name + ".html";
	}

	get_all_templates_in = function(folder_name) {
		return "app/templates/" + folder_name + "/**/*.html";
	}

	insert_data = function(page) {
		return {
			src: "generated/pages_temp/"+page+".html",
			dest: "generated/pages/"+page+".html",
			match: "<!-- insert data here -->"
		}
	}

	// List of features available in the cloudos app
	var cloudos_app_files = [
		"app/js/apps/cloudos-base.js",
		get_feature("login"),
		get_feature("logout"),
		get_feature("settings"),
		get_feature("profile"),
		get_feature("reset_password"),
		get_feature("two_factor_verification"),
		get_feature("request_message"),
		get_feature("using_apps")
	];

	// List of features available in the admin app
	var admin_app_files = [
		"app/js/apps/admin-base.js",
		get_feature("logout"),
		get_feature("request_message"),
		get_feature("using_apps"),
		get_feature("app_store"),
		get_feature("accounts"),
		get_feature("email_domains"),
		get_feature("aliases"),
		get_feature("ssl_certificates"),
		get_feature("groups"),
		get_feature("valet_keys"),
		get_feature("installed_apps_settings"),
		get_feature("task_progress"),
	];

	// List of features available in the setup app
	var setup_app_files = [
		"app/js/apps/setup-base.js",
		get_feature("cloudos_restore")
	];


	// List of features templates in the cloudos app
	var cloudos_app_templates = [
		get_all_templates_in("index"),
		get_template("components/task_progress_modal"),
		get_template("components/modal_dialog")
	];

	// List of features templates in the admin app
	var admin_app_templates = [
		get_all_templates_in("admin"),
		get_template("components/modal_dialog_alt")
	];

	// List of features templates in the setup app
	var setup_app_templates = [
		get_all_templates_in("setup"),
		get_template("components/modal_dialog_alt")
	];



	//Override application configuration here. Common examples follow in the comments.
	return {

		loadNpmTasks: lineman.config.application.loadNpmTasks.concat("grunt-insert", "grunt-contrib-concat"),

		removeTasks:{
			common: ["jshint","handlebars"],
		},
		appendTasks:{
			dist: ["concat", "copy:local","copy:csapp_gen", "copy:templates", "insert", "copy:pages", "copy:pages_dist","copy:csapp_dist","copy:build"],
		},
		concat: {
			options:
			{
				sourcesContent: true,
				sourceMappingURL: ""
			},
			gen_apps: {
				files: {
					"generated/js/apps/cloudos-app.js": cloudos_app_files,
					"generated/js/apps/admin-app.js": admin_app_files,
					"generated/js/apps/setup-app.js": setup_app_files
				}
			},
			gen_templates: {
				files: {
					"generated/pages_temp/index.html": cloudos_app_templates,
					"generated/pages_temp/admin.html": admin_app_templates,
					"generated/pages_temp/setup.html": setup_app_templates
				}
			}
		},
		copy: {
			local: {
				files:[
						{	expand: true,
							cwd: 'app/js/local/',
							src: '**/*',
							dest: 'dist/js/local',
							}
				]
			},
			csapp_dist: {
				files:[
						{	expand: true,
							cwd: 'generated/js/apps',
							src: '*app*.js',
							dest: 'dist/js/apps',
							}
				]
			},
			csapp_gen: {
				files:[
						{	expand: true,
							cwd: 'app/js/apps/',
							src: '*app*',
							dest: 'generated/js/apps',
							}
				]
			},
			templates: {
				files:[
					{	expand: true,
						cwd: 'app/templates/apps/',
						src: '**/*.html',
						dest: 'generated/pages',
					}
				]
			},
			pages: {
				files:[
						{	expand: true,
							cwd: 'generated/pages/',
							src: '**/*.html',
							dest: 'generated',
							}
				]
			},
			pages_dist: {
				files:[
						{	expand: true,
							cwd: 'generated/',
							src: '*.html',
							dest: 'dist',
							}
				]
			},
			build: {
				files:[
						{	expand: true,
							cwd: 'dist',
							src: '**/*',
							dest: '../target/classes/static',
							}
				]
			}
		},
		insert: {
			options: {},
			admin: insert_data("admin"),
			index: insert_data("index"),
			setup: insert_data("setup")
		}
	};
};
