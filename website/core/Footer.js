/**
 * Copyright (c) 2017-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

const React = require('react');

class Footer extends React.Component {
    docUrl(doc, language) {
        const baseUrl = this.props.config.baseUrl;
        const docsUrl = this.props.config.docsUrl;
        const docsPart = `${docsUrl ? `${docsUrl}/` : ''}`;
        const langPart = `${language ? `${language}/` : ''}`;
        return `${baseUrl}${docsPart}${langPart}${doc}`;
    }

    pageUrl(doc, language) {
        const baseUrl = this.props.config.baseUrl;
        return baseUrl + (language ? `${language}/` : '') + doc;
    }

    render() {
        return (
            <footer className="nav-footer" id="footer">
                <section className="sitemap">
                    <a href={this.props.config.baseUrl} className="nav-home">
                        {this.props.config.footerIcon && (
                            <img
                                src={this.props.config.baseUrl + this.props.config.footerIcon}
                                alt={this.props.config.title}
                                width="66"
                                height="66"
                            />
                        )}
                    </a>
                    <div>
                        <h5>Docs</h5>
                        <a href={this.docUrl('index.html', this.props.language)}>
                            Documentation
                        </a>
                    </div>
                    <div>
                        <h5>Community</h5>
                        <a href="https://gitter.im/wvlet/airframe">Gitter Chat</a>
                    </div>
                    <div>
                        <h5>More</h5>
                        <a href="https://github.com/wvlet/airframe/">GitHub</a>
                        <a
                            className="github-button"
                            href={this.props.config.repoUrl}
                            data-icon="octicon-star"
                            data-count-href="/wvlet/airframe/stargazers"
                            data-show-count="true"
                            data-count-aria-label="# stargazers on GitHub"
                            aria-label="Star this project on GitHub">
                            Star
                        </a>
                    </div>
                </section>

                <a
                    href="https://wvlet.org/airframe/"
                    target="_blank"
                    rel="noreferrer noopener"
                    className="fbOpenSource">
                    <img
                        src={`${this.props.config.baseUrl}img/logos/airframe-badge-dark.png`}
                        alt="airframe logo"
                    />
                </a>
                <section className="copyright">{this.props.config.copyright}</section>
            </footer>
        );
    }
}

module.exports = Footer;
