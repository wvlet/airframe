/** @type {import('@docusaurus/plugin-content-docs').SidebarsConfig} */
const sidebars = {
  docs: [
    {
      type: 'category',
      label: 'Resources',
      collapsible: false,
      items: ['index', 'airframe-walkthrough', 'articles', 'release-notes', 'logos'],
    },
    {
      type: 'category',
      label: 'Framework',
      collapsible: false,
      items: ['airframe-di', 'airframe-rpc', 'airframe-http', 'airframe-rx', 'airspec'],
    },
    {
      type: 'category',
      label: 'Core Modules',
      collapsible: false,
      items: [
        'airframe-codec',
        'airframe-config',
        'airframe-control',
        'airframe-log',
        'airframe-metrics',
        'airframe-surface',
      ],
    },
    {
      type: 'category',
      label: 'Utilities',
      collapsible: false,
      items: [
        'airframe-benchmark',
        'airframe-canvas',
        'airframe-fluentd',
        'airframe-http-recorder',
        'airframe-jdbc',
        'airframe-jmx',
        'airframe-json',
        'airframe-launcher',
        'airframe-msgpack',
        'airframe-parquet',
        'airframe-sql',
        'airframe-ulid',
      ],
    },
  ],
};

export default sidebars;
