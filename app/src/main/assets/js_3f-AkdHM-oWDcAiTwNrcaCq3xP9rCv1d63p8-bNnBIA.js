/* Source and licensing information for the line(s) below can be found at https://www.egd.cz/modules/contrib/search_api_autocomplete/js/search_api_autocomplete.js. */
(function($, Drupal, drupalSettings, once) {
    'use strict';
    if (!Drupal.autocomplete)
        return;
    var autocomplete = {};
    autocomplete.getSettings = function(input, globalSettings) {
        globalSettings = globalSettings || drupalSettings || {};
        var settings = {
            auto_submit: false,
            delay: 0,
            min_length: 1,
            selector: ':submit'
        }
          , search = $(input).data('search-api-autocomplete-search');
        if (search && globalSettings.search_api_autocomplete && globalSettings.search_api_autocomplete[search])
            $.extend(settings, globalSettings.search_api_autocomplete[search]);
        return settings
    }
    ;
    Drupal.behaviors.searchApiAutocomplete = {
        attach: function(context, settings) {
            $(once('search-api-autocomplete', '.ui-autocomplete-input[data-search-api-autocomplete-search]', context)).each(function() {
                var uiAutocomplete = $(this).data('ui-autocomplete');
                if (!uiAutocomplete)
                    return;
                var $element = uiAutocomplete.menu.element;
                $element.data('search-api-autocomplete-input-id', this.id);
                $element.addClass('search-api-autocomplete-search');
                var elementSettings = autocomplete.getSettings(this, settings);
                if (elementSettings.delay)
                    uiAutocomplete.options['delay'] = elementSettings.delay;
                if (elementSettings.min_length)
                    uiAutocomplete.options['minLength'] = elementSettings.min_length;
                var oldSelect = uiAutocomplete.options.select;
                uiAutocomplete.options.select = function(event, ui) {
                    if (ui.item.url) {
                        location.href = ui.item.url;
                        return false
                    }
                    ;var ret = oldSelect.apply(this, arguments);
                    if (elementSettings.auto_submit && elementSettings.selector)
                        $(elementSettings.selector, this.form).trigger('click');
                    return ret
                }
            })
        }
    };
    Drupal.SearchApiAutocomplete = autocomplete
}
)(jQuery, Drupal, drupalSettings, once)/* Source and licensing information for the above line(s) can be found at https://www.egd.cz/modules/contrib/search_api_autocomplete/js/search_api_autocomplete.js. */
;
