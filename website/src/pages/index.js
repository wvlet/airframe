import Link from '@docusaurus/Link';
import useBaseUrl from '@docusaurus/useBaseUrl';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import Layout from '@theme/Layout';

const features = [
  {
    title: 'Designed for Scala, Scala.js, and Scala Native',
    image: 'img/features/scala-logo-red-spiral-dark.png',
    description: (
      <>
        Airframe extends capability of Scala to the next level. Have you ever used{' '}
        <a href="http://slf4j.org/">slf4j</a> (logging), <a href="https://github.com/FasterXML/jackson">Jackson</a>{' '}
        (JSON-based serialization), <a href="https://github.com/google/guice">Guice</a> (dependency injection)? Airframe
        has redesigned these Java-based ecosystem as <Link to="/docs/airframe-log">airframe-log</Link>,{' '}
        <Link to="/docs/airframe-codec">airframe-codec</Link>, and <Link to="/docs/airframe-di">airframe-di</Link> in
        order to maximize the power of Scala. Scala 3 is also fully supported, as well as Scala.js and Scala Native.
      </>
    ),
  },
  {
    title: 'RPC and REST Framework',
    image: 'img/airframe-rpc/rpc-overview.png',
    description: (
      <>
        <Link to="/docs/airframe-rpc">Airframe RPC</Link> supports seamless integration of server and clients using
        Scala as RPC interfaces. You can also define plain <Link to="/docs/airframe-http">REST</Link> interfaces.
      </>
    ),
  },
  {
    title: 'Simple Testing Framework',
    image: 'img/features/airspec.png',
    description: (
      <>
        <Link to="/docs/airspec">AirSpec</Link> is a simple unit testing framework. No need to remember complex DSLs
        for writing tests in Scala.
      </>
    ),
  },
  {
    title: 'Object Serialization',
    image: 'img/features/msgpack.png',
    description: (
      <>
        Airframe uses <Link to="/docs/airframe-codec">MessagePack-based schema-on-read codec</Link> for fast and
        compact object serialization. <Link to="/docs/airframe-json">JSON</Link>-based serialization is also supported.
      </>
    ),
  },
  {
    title: 'Time Series Data Management',
    image: 'img/features/undraw_time_management_30iu.svg',
    description: (
      <>
        <Link to="/docs/airframe-metrics">airframe-metrics</Link> provides human-friendly time range selectors (e.g.,
        -1d, -1w) and data units for measuring elapsed time (e.g., 0.1s, 1.5h) and data sizes (e.g., GB, PB.)
      </>
    ),
  },
  {
    title: 'Fluentd Logging',
    image: 'img/features/Fluentd_square.svg',
    description: (
      <>
        <Link to="/docs/airframe-fluentd">airframe-fluentd</Link> supports logging your metrics to fluentd in a
        type-safe manner. You just need to send your case classes as metrics for fluentd.
      </>
    ),
  },
];

function Feature({title, image, description}) {
  return (
    <div className="col col--6 feature">
      <img src={useBaseUrl(image)} alt="" />
      <h3>{title}</h3>
      <p>{description}</p>
    </div>
  );
}

export default function Home() {
  const {siteConfig} = useDocusaurusContext();
  return (
    <Layout description={siteConfig.tagline}>
      <header className="hero hero--airframe">
        <div className="container">
          <img className="hero__logo" src={useBaseUrl('img/logos/airframe-logo-tr.png')} alt="Airframe logo" />
          <h1 className="hero__title">{siteConfig.title}</h1>
          <p className="hero__subtitle">{siteConfig.tagline}</p>
          <div className="hero__buttons">
            <Link className="button button--secondary button--lg" to="/docs">
              Get Started
            </Link>
            <Link className="button button--outline button--secondary button--lg" href="https://github.com/wvlet/airframe/">
              GitHub
            </Link>
          </div>
        </div>
      </header>
      <main className="container features">
        <div className="row">
          {features.map((props) => (
            <Feature key={props.title} {...props} />
          ))}
        </div>
      </main>
    </Layout>
  );
}
