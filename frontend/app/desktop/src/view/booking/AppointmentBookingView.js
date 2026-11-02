Ext.define('IcaresCodingChallenge.view.booking.AppointmentBookingView', {
    extend: 'Ext.Container',
    xtype: 'appointmentbookingview',
    cls: 'appointmentbookingview',
    controller: {
        type: 'appointmentbookingviewcontroller'
    },
    requires: [
        'Ext.field.Date',
        'Ext.field.Time'
    ],
    scrollable: true,
    layout: {
        type: 'vbox',
        align: 'stretch'
    },
    items: [
        {
            xtype: 'component',
            html: '<h1>Termine buchen</h1>'
        },
        {
            xtype: 'container',
            cls: 'appointmentpanel',
            flex: 1,
            layout: {
                type: 'vbox',
                align: 'stretch'
            },
            items: [
                {
                    xtype: 'providerserviceselection',
                    reference: 'providerServiceSelection'
                },
                {
                    xtype: 'container',
                    layout: {
                        type: 'hbox',
                        align: 'start'
                    },
                    items: [
                        {
                            xtype: 'datefield',
                            reference: 'fromDateField',
                            label: 'Startdatum',
                            dateFormat: 'd.m.Y',
                            flex: 1,
                            margin: '0 8 0 0'
                        },
                        {
                            xtype: 'timefield',
                            reference: 'fromTimeField',
                            label: 'Startzeit',
                            format: 'H:i',
                            flex: 1,
                            margin: '0 16 0 0'
                        },
                        {
                            xtype: 'datefield',
                            reference: 'toDateField',
                            label: 'Enddatum',
                            dateFormat: 'd.m.Y',
                            flex: 1,
                            margin: '0 8 0 0'
                        },
                        {
                            xtype: 'timefield',
                            reference: 'toTimeField',
                            label: 'Endzeit',
                            format: 'H:i',
                            flex: 1
                        }
                    ]
                },
                {
                    xtype: 'button',
                    text: 'Termine vorschlagen',
                    handler: 'onSuggestAppointments',
                    margin: '16 0 0 0'
                },
                {
                    xtype: 'grid',
                    reference: 'suggestionsGrid',
                    flex: 1,
                    margin: '16 0 0 0',
                    hidden: true,
                    emptyText: 'Keine verfügbaren Termine im gewählten Zeitraum gefunden.',
                    store: {
                        type: 'appointmentsuggestions'
                    },
                    columns: [
                        {
                            text: 'Beginn',
                            dataIndex: 'start',
                            flex: 1,
                            renderer: function (value) {
                                return Ext.Date.format(value, 'd.m.Y H:i');
                            }
                        },
                        {
                            text: 'Ende',
                            dataIndex: 'end',
                            flex: 1,
                            renderer: function (value) {
                                return Ext.Date.format(value, 'd.m.Y H:i');
                            }
                        }
                    ]
                }
            ]
        }
    ]
});
