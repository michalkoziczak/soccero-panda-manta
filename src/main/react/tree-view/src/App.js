import React, { Component } from 'react';
import Graph from 'vis-react';
import ReactDropdown from 'react-dropdown';
import 'react-dropdown/style.css'
import _ from 'lodash'
import './App.css';

var events = {
    select: function(event) {
        var { nodes, edges } = event;
    }
}

class App extends Component {

    constructor(props) {
        super(props);

        var options = this.generateGraphOptions()

        this.state = {
            selectedGraph: {nodes:[], edges:[]},
            options: options,
            competitions: [],
            graphs: {}
        }
    }

   componentDidMount() {
       this.attachWebSocket(true);
       window.onresize = this.handleResize.bind(this);
       this.handleResize();
   }

   handleResize() {
       this.setState({options: this.generateGraphOptions()});
   }

   generateGraphOptions() {
     var w = window;
     var e = document.documentElement;
     var selector = document.getElementById('graph-selector');
     var g = document.getElementsByTagName('body')[0];

     var pageX = w.innerWidth || e.clientWidth || g.clientWidth;
     var pageY = w.innerHeight|| e.clientHeight || g.clientHeight;

     var selectorY = 0;

     if (selector !== null) {
         selectorY = selector.clientHeight || 0;
     }

     var x = pageX;
     var y = pageY - selectorY;

     return {
          layout: {
              hierarchical: {
                direction: "LR",
                nodeSpacing: 40,
                treeSpacing: 50,
                levelSeparation: 500
              }
          },
          edges: {
              color: "#000000"
          },
          height: y + 'px',
          width: x + 'px',
          physics: {enabled: false}
     };
   }

   attachWebSocket(init) {
       var websocket = new WebSocket("ws://soccero-panda-manta.dev.kende.pl/tournaments");
       websocket.onmessage = this.handleTournamentChange.bind(this);
       websocket.onclose = this.attachWebSocket.bind(this);
       if (init === true) {
           websocket.onopen = function() {
               websocket.send("init")
           };
       }

   }

   handleTournamentChange(event) {
     var tree = JSON.parse(event.data);
     var graphs = this.state.graphs;

     var nodes = _.map(tree.nodes, function(node) {
        var color = undefined;

        if (node.state === 'WON') {
         color = "#d3ffd1";
        }

        if (node.state === 'LOST') {
         color = "#ffc600";
        }

        return {
            id: node.id,
            label: node.label,
            level: node.round,
            color: color,
            shape: 'box'
        };
     });

    var edges = _.map(tree.nodes, function(node) {
      if (!node.child) {
        return null;
      }
      return {from: node.id, to: node.child};
    });
    var edges = _.filter(edges, function(edge) {return edge !== null});
    var graph = {nodes: nodes, edges: edges};

    graphs[tree.leagueName + " " + tree.competition] = graph;
    var competitions = _.keys(graphs);

    this.setState({
        graphs: graphs,
        competitions: competitions
    });
   }

   _onSelect(e) {
     var graphs = this.state.graphs || {};
     var graph = graphs[e.value];
     if (!graph) {
       this.setState({selectedGraph: {nodes:[], edges:[]}});
       return;
     }

     this.setState({selectedGraph: graph, selectedCompetition: e.value});
   }

  render() {
    var selectedCompetition = this.state.selectedCompetition;
    var selectedGraph = this.state.selectedGraph;
    if (!this.state.selectedCompetition && this.state.competitions.length > 0) {
      selectedCompetition = this.state.competitions[0];
      selectedGraph = this.state.graphs[selectedCompetition];
    }

    return (
      <div>
      <div id="graph-selector"><ReactDropdown value={selectedCompetition} options={this.state.competitions} placeholder="Select competition" onChange={this._onSelect.bind(this)}></ReactDropdown></div>
      <div id="graph-view"><Graph graph={selectedGraph} options={this.state.options} events={events}  /></div>
      </div>

    );
  }
}

export default App;
