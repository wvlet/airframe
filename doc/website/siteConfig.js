/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Copyright (c) 2017-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

// See https://docusaurus.io/docs/site-config for all the possible
// site configuration options.

// List of projects/orgs using your project for the users page.
const users = [
    {
        caption: 'Arm Treasure Data',
        // You will need to prepend the image path with your baseUrl
        // if it is not '/', like: '/test-site/img/image.jpg'.
        image: '/img/undraw_open_source.svg',
        infoLink: 'https://www.treasuredata.com',
        pinned: true,
    },
];

const siteConfig = {
    title: 'Airframe', // Title for your website.
    tagline: 'Essential Building Blocks For Scala',
    url: 'https://wvlet.org', // Your website URL
    baseUrl: '/airframe/', // Base URL for your project */
    // For github.io type URLs, you would set the url and baseUrl like:
    //   url: 'https://facebook.github.io',
    //   baseUrl: '/test-site/',

    // Used for publishing and more
    projectName: 'airframe',
    organizationName: 'wvlet.org',
    // For top-level user or org sites, the organization is still the same.
    // e.g., for the https://JoelMarcey.github.io site, it would be set like...
    //   organizationName: 'JoelMarcey'

    // For no header links in the top nav bar -> headerLinks: [],
    headerLinks: [
        {href: '/airframe/docs/', label: 'Docs'},
        {href: 'https://github.com/wvlet/airframe/', label: 'GitHub'},
    ],

    // If you have users set above, you add it here:
    users,

    /* path to images for header/footer */
    headerIcon: 'img/favicon.ico',
    footerIcon: 'img/favicon.ico',
    favicon: 'img/favicon.ico',

    /* Colors for website */
    colors: {
        primaryColor: '#07405b',
        secondaryColor: '#0e859b',
    },

    /* Custom fonts for website */
    /*
    fonts: {
        myFont: [
            "Georgia",
            "goudy old style", "'minion pro", "'bell mt", "'Hiragino Kaku Gothic ProN", "serif",
            "ヒラギノ角ゴ ProN W3", "メイリオ", "Meiryo"
        ],
        myOtherFont: [
            "-apple-system",
            "system-ui"
        ]
    },
    */

    // This copyright info is used in /core/Footer.js and blog RSS/Atom feeds.
    copyright: `Copyright © ${new Date().getFullYear()} wvlet.org`,

    highlight: {
        // Highlight.js theme to use for syntax highlighting in code blocks.
        theme: 'default',
    },

    // Add custom scripts here that would be placed in <script> tags.
    scripts: ['https://buttons.github.io/buttons.js'],

    // On page navigation for the current documentation page.
    onPageNav: 'separate',
    // No .html extensions for paths.
    cleanUrl: true,

    // Open Graph and Twitter card images.
    ogImage: 'https://wvlet.org/airframe/img/poster.png',
    twitterImage: 'https://wvlet.org/airframe/img/poster.png',

    // For sites with a sizable amount of content, set collapsible to true.
    // Expand/collapse the links and subcategories under categories.
    // docsSideNavCollapsible: true,

    // Show documentation's last contributor's name.
    // enableUpdateBy: true,

    // Show documentation's last update time.
    // enableUpdateTime: true,

    // You may provide arbitrary config keys to be used as needed by your
    // template. For example, if you need your repo's URL...
    repoUrl: 'https://github.com/wvlet/airframe',
};

module.exports = siteConfig;
