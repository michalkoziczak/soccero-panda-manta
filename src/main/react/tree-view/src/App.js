import React, { Component } from 'react';
import Graph from 'vis-react';
import ReactDropdown from 'react-dropdown';
import 'react-dropdown/style.css'
import _ from 'lodash'
import './App.css';

var w = window,
d = document,
e = d.documentElement,
g = d.getElementsByTagName('body')[0],
x = w.innerWidth || e.clientWidth || g.clientWidth,
y = w.innerHeight|| e.clientHeight|| g.clientHeight;

var events = {
    select: function(event) {
        var { nodes, edges } = event;
    }
}

class App extends Component {

    constructor(props) {
        super(props);

        var options = {
             layout: {
                 hierarchical: true
             },
             edges: {
                 color: "#000000"
             },
             height: y + 'px',
             width: '100%'
        };

        this.state = {
            selectedGraph: {nodes:[], edges:[]},
            options: options,
            competitions: [],
            graphs: {}
        }
    }

   componentDidMount() {
       this.attachWebSocket(true);
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

        return {id: node.id, label: node.label, level: node.round, color: color};
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

    var selectedCompetition = this.state.selectedCompetition;
    var selectedGraph = this.state.selectedGraph;
    if (!this.state.selectedCompetition) {
      selectedCompetition = tree.leagueName + " " + tree.competition;
      selectedGraph = graph;
    }

    this.setState({
        graphs: graphs,
        competitions: competitions,
        selectedCompetition: selectedCompetition,
        selectedGraph: graph
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
    return (
      <div>
      <ReactDropdown value={this.state.selectedCompetition} options={this.state.competitions} placeholder="Select competition" onChange={this._onSelect.bind(this)}></ReactDropdown>
      <Graph graph={this.state.selectedGraph} options={this.state.options} events={events}  />
      </div>

    );
  }
}

export default App;
