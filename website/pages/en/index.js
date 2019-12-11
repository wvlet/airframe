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
                className="twoColumn">
                <div>
                    <img width='200px' src={`${baseUrl}img/logos/airframe-logo-tr.png`} alt="Project Logo"/>
                </div>
            </Container>
        );

        const Features = ({background = 'light'}) => (
            <Block layout="threeColumn">
                {[
                    {
                        content: 'Airframe has redesigned everything for Scala and can be a replacement of the commonly-used libraries in Java, such as Jackson, Google Guava, Guice, slf4j, etc. This is useful for minimizing the dependency hell of these libraries. Airframe also supports Scala.js as most of the components are written purely in Scala.',
                        image: `${baseUrl}img/features/scala-logo-red-spiral-dark.png`,
                        imageAlign: 'top',
                        title: 'Designed for Scala',
                    },
                    {
                        content:
                            'Airframe uses [MessagePack-based schema-on-read codec](docs/airframe-codec) for fast and compact object serialization. It can be used for efficient HTTP server-client communication, and automatically resolves differences between data types (e.g., integers and strings.). Serialization with [JSON](docs/airframe-json) is also supported. ',
                        image: `${baseUrl}img/features/msgpack.png`,
                        imageAlign: 'top',
                        title: 'MessagePack Serialization',
                    },
                    {
                        content: 'With [dependency injection (DI)](docs/airframe) of Airframe, building services with hundreds of module classes becomes easy and manageable. Airframe DI will build complex objects on your behalf by following [your custom design](docs/airframe#design) and properly [start and shutdown your services in FILO order](docs/airframe#life-cycle).',
                        image: `${baseUrl}img/features/di.png`,
                        imageAlign: 'top',
                        title: 'Dependency Injection'
                    },
                    {
                        content: 'Logging is an essential tool for debugging applications. [airframe-log](docs/airframe-log) helps displaying logs with fancy ANSI colors enriched with the source code locations.',
                        image: `${baseUrl}img/airframe-log/demo.png`,
                        imageAlign: 'top',
                        title: 'Colorful Source Code Logging',
                    },
                    {
                        content: '[airframe-config](docs/airframe-config) supports YAML-based application configurations and provides immutable config objects that can be injected through DI. Partially overriding configutations is also supported.',
                        image: `${baseUrl}img/immutable_config.png`,
                        imageAlign: 'top',
                        title: 'Application Config Flow',
                    },
                    {
                        content: 'Need a way to parse command line options? [airframe-launcher](docs/airframe-launcher) is a handy command line parser that can be used to call Scala functions from the command line arguments.',
                        image: `${baseUrl}/img/features/launcher.png`,
                        imageAlign: 'top',
                        title: 'Command-Line Parser'
                    },
                    {
                        content: '[AirSpec](docs/airspec) is a simple unit testing framework for Scala and Scala.js. You can uses public functions in your classes as test cases. No need to remember complex DSLs to start writing tests in Scala.',
                        image: `${baseUrl}/img/features/airspec.png`,
                        imageAlign: 'top',
                        title: 'Simple Testing Framework'
                    },
                    {
                        content: 'Retrying HTTP requests for API calls is an essential technique for connecting microservices. [airframe-control](docs/airframe-control) provides essential tools for making your requests reliable with exponential backoff retry, jitter, circuit-breaker, rate control, etc.',
                        image: `${baseUrl}/img/features/undraw_process_e90d.svg`,
                        imageAlign: 'top',
                        title: 'Retry, Rate Control'
                    },
                    {
                        content: "[airframe-http](docs/airframe-http) allows building web services by using Scala as an IDL (Interface Definition Language). Airframe provides a ready-to use web server implementation based on [Twitter Finagle](https://twitter.github.io/finagle/guide/) and built-in JSON/MessagePack-based REST API call mapping to crate microservice API servers and clients at ease",
                        image: `${baseUrl}/img/features/finagle.png`,
                        imageAlign: 'top',
                        title: 'Web Service IDL in Scala'
                    },
                    {
                        content: "Developing web applications needs efficient HTTP clients and mock servers even if the network is disconnected. With [airframe-http-recorder](docs/airframe-http-recorder) you can record real server responses and replay them for running unit tests even if the servers are offline.",
                        image: `${baseUrl}/img/features/undraw_server_down_s4lk.svg`,
                        imageAlign: 'top',
                        title: 'HTTP Client and Recorders',
                    },
                    {
                        content: `[airframe-fluentd](docs/airframe-fluentd) supports logging your metrics to fluentd. Collecting metrics is much easier with the type-safe logging of airframe-fluentd.`,
                        image: `${baseUrl}/img/features/Fluentd_square.svg`,
                        imageAlign: 'top',
                        title: 'Fluentd Logging',
                    },
                    {
                        content: "[airframe-metrics](docs/airframe-metrics) provides human-friendly time range selectors (e.g., -1d, -1w) and data units for measuring elapsed time (e.g., 0.1s, 1.5h) and data sizes (e.g., GB, PB.)",
                        image: `${baseUrl}/img/features//undraw_time_management_30iu.svg`,
                        imageAlign: 'top',
                        title: 'Time Series Data Management',
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
