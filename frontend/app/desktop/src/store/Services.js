Ext.define('IcaresCodingChallenge.store.Services', {
    extend: 'Ext.data.Store',
    alias: 'store.services',

    fields: [
        { name: 'serviceId', type: 'int' },
        { name: 'code', type: 'string' },
        { name: 'name', type: 'string' },
        { name: 'durationMinutes', type: 'int' }
    ],

    autoLoad: false,

    proxy: {
        type: 'ajax',
        url: '/service-providers',
        reader: {
            type: 'json',
            rootProperty: ''
        }
    }
});
