Ext.define('IcaresCodingChallenge.store.Providers', {
    extend: 'Ext.data.Store',
    alias: 'store.providers',

    fields: [
        { name: 'providerId', type: 'int' },
        { name: 'name', type: 'string' },
        { name: 'capacity', type: 'int' }
    ],

    autoLoad: false,

    proxy: {
        type: 'ajax',
        url: (window.API_BASE_URL || '') + '/service-providers',
        reader: {
            type: 'json',
            rootProperty: ''
        }
    }
});
