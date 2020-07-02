/**
 * Copyright (c) 2017-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

const React = require('react');

const CompLibrary = require('../../core/CompLibrary.js');

const MarkdownBlock = CompLibrary.MarkdownBlock; /* Used to read markdown */
const Container = CompLibrary.Container;
const GridBlock = CompLibrary.GridBlock;

class HomeSplash extends React.Component {
    render() {
        const {siteConfig, language = ''} = this.props;
        const {baseUrl, docsUrl} = siteConfig;
        const docsPart = `${docsUrl ? `${docsUrl}/` : ''}`;
        const langPart = `${language ? `${language}/` : ''}`;
        const docUrl = doc => `${baseUrl}${docsPart}${langPart}${doc}`;

        const SplashContainer = props => (
            <div className="homeContainer">
                <div className="homeSplashFade">
                    <div className="wrapper homeWrapper">{props.children}</div>
                </div>
            </div>
        );

        const ProjectTitle = () => (
            <h2 className="projectTitle">
                {siteConfig.title}
                <small>{siteConfig.tagline}</small>
            </h2>
        );

        const PromoSection = props => (
            <div className="section promoSection">
                <div className="promoRow">
                    <div className="pluginRowBlock">
                        {props.children}
                    </div>
                </div>
            </div>
        );

        const Button = props => (
            <div className="pluginWrapper buttonWrapper">
                <a className="button" href={props.href} target={props.target}>
                    {props.children}
                </a>
            </div>
        );

        return (
            <SplashContainer>
                <div className="inner">
                    <ProjectTitle siteConfig={siteConfig}/>

                    <PromoSection>
                        <Button href='./docs'>Get Started</Button>
                        <Button href="https://github.com/wvlet/airframe/">GitHub</Button>

                        <a className="github-button" href="https://github.com/wvlet/airframe"
                           data-color-scheme="no-preference: light; light: light; dark: dark;" data-size="large"
                           data-show-count="true" aria-label="Star wvlet/airframe on GitHub">Star</a>
                    </PromoSection>
                </div>
            </SplashContainer>
        );
    }
}

class Index extends React.Component {
    render() {
        const {config: siteConfig, language = ''} = this.props;
        const {baseUrl} = siteConfig;

        const Logo = props => (
            <div className="projectLogo">

            </div>
        );


        const Block = props => (
            <Container
                padding={['bottom', 'top']}
                id={props.id}
                background={props.background}>
                <GridBlock
                    align="center"
                    contents={props.children}
                    layout={props.layout}
                />
            </Container>
        );

        const FeatureCallout = () => (
            <Container
                padding={['bottom', 'top']}
                background="light"
                className="threeColumn">
                <div>
                    <img width='200px' src={`${baseUrl}img/logos/airframe-logo-tr.png`} alt="Project Logo"/>
                </div>
            </Container>
        );

        const Features = ({background = 'light'}) => (
            <Block layout="threeColumn">
                {[
                    {
                        content: '[Airframe RPC](docs/airframe-rpc) supports seamless integration of server and clients using Scala as RPC interfaces.',
                        image: `${baseUrl}img/airframe-rpc/rpc-overview.png`,
                        imageAlign: 'top',
                        title: 'RPC Framework',
                    },
                    {
                        content: 'Have you ever used [slf4](http://slf4j.org/) (logging), [Jackson](https://github.com/FasterXML/jackson) (JSON-based serialization), [Guice](https://github.com/google/guice) (dependency injection)? ' +
                            'Airframe has redesigned these Java-based ecosystem as [airframe-logging](docs/airframe-log.md), [airframe-codec](docs/airframe-codec.md), and [airframe-di](docs/airframe.md) in order to maximize the power of Scala and Scala.js',
                        image: `${baseUrl}img/features/scala-logo-red-spiral-dark.png`,
                        imageAlign: 'top',
                        title: 'Designed for Scala and Scala.js',
                    },
                    {
                        content:
                            'Airframe uses [MessagePack-based schema-on-read codec](docs/airframe-codec) for fast and compact object serialization. [JSON](docs/airframe-json) serialization is also supported.',
                        image: `${baseUrl}img/features/msgpack.png`,
                        imageAlign: 'top',
                        title: 'MessagePack-based Object Serialization',
                    },
                    {
                        content: '[AirSpec](docs/airspec) is a simple unit testing framework for Scala and Scala.js. You can use public methods in your classes as test cases. No need to remember complex DSLs for writing tests in Scala.',
                        image: `${baseUrl}/img/features/airspec.png`,
                        imageAlign: 'top',
                        title: 'Simple Testing Framework'
                    },
                    {
                        content: "[airframe-http](docs/airframe-http) supports building REST web services by using Scala as an IDL (Interface Definition Language). Airframe provides a ready-to use web server implementation based on [Twitter Finagle](https://twitter.github.io/finagle/guide/) and built-in JSON/MessagePack-based REST API call mapping to crate microservice API servers and clients at ease",
                        image: `${baseUrl}/img/features/finagle.png`,
                        imageAlign: 'top',
                        title: 'REST Services'
                    },
                    {
                        content: 'Retrying HTTP requests for API calls is an essential technique for connecting microservices. [airframe-control](docs/airframe-control) will provide essential tools for making your requests reliable with exponential backoff retry, jitter, circuit-breaker, rate control, etc.',
                        image: `${baseUrl}/img/features/undraw_process_e90d.svg`,
                        imageAlign: 'top',
                        title: 'Retry, Rate Control'
                    },
                    {
                        content: "Web application development often requires mock web servers. With [airframe-http-recorder](docs/airframe-http-recorder), you can record and replay the real server responses for running unit tests even if the servers are offline or unreachable from CI environments.",
                        image: `${baseUrl}/img/features/undraw_server_down_s4lk.svg`,
                        imageAlign: 'top',
                        title: 'HTTP Client and Recorders',
                    },
                    {
                        content: "[airframe-metrics](docs/airframe-metrics) provides human-friendly time range selectors (e.g., -1d, -1w) and data units for measuring elapsed time (e.g., 0.1s, 1.5h) and data sizes (e.g., GB, PB.)",
                        image: `${baseUrl}/img/features//undraw_time_management_30iu.svg`,
                        imageAlign: 'top',
                        title: 'Time Series Data Management',
                    },
                    {
                        content: `[airframe-fluentd](docs/airframe-fluentd) supports logging your metrics to fluentd in a type-safe manner. You just need to send your case classes as metrics for fluentd.`,
                        image: `${baseUrl}/img/features/Fluentd_square.svg`,
                        imageAlign: 'top',
                        title: 'Fluentd Logging',
                    },
                ]}
            </Block>
        );

        return (
            <div>
                <HomeSplash siteConfig={siteConfig} language={language}/>
                <div className="mainContainer">
                    <Features/>
                </div>
            </div>
        );
    }
}

module.exports = Index;
