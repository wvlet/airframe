import React from 'react';
import clsx from 'clsx';
import Link from '@docusaurus/Link';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import Layout from '@theme/Layout';
import HomepageFeatures from '@site/src/components/HomepageFeatures';

import styles from './index.module.css';

function HomepageHeader() {
    const {siteConfig} = useDocusaurusContext();

    const SplashContainer = props => (
        <header className={clsx('hero hero--primary', styles.heroBanner)}>
            <div className="container">
                <h1 className="hero__title">{siteConfig.title}</h1>
                <p className="hero__subtitle">{siteConfig.tagline}</p>
                <div className={styles.buttons}>
                  {props.children}
                </div>
            </div>
        </header>
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
        <Link className="button button--primary" to={props.href} target={props.target}>
            {props.children}
        </Link>
    );

  return (
      <SplashContainer>
          <div className="inner">
              <PromoSection>
                  <Button href='./docs'>Get Started</Button>
                  &nbsp;
                  <Button href="https://github.com/wvlet/airframe/">GitHub</Button>
                  <a className="github-button"
                     href="https://github.com/wvlet/airframe"
                     data-color-scheme="no-preference: light; light: light; dark: dark;" data-size="large"
                     data-show-count="true" aria-label="Star wvlet/airframe on GitHub">Star</a>
              </PromoSection>
          </div>
      </SplashContainer>
  );
}

export default function Home() {
  const {siteConfig} = useDocusaurusContext();
  return (
    <Layout
      title={`Hello from ${siteConfig.title}`}
      description="Description will go into a meta tag in <head />">
      <HomepageHeader />
      <main>
        <HomepageFeatures />
      </main>
    </Layout>
  );
}
